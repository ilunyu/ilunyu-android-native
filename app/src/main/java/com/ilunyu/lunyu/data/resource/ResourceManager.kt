package com.ilunyu.lunyu.data.resource

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipInputStream

class ResourceManager(
    private val context: Context,
    private val resourceRepository: ResourceRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val root = File(context.filesDir, "resources")
    private val downloads = File(root, "downloads")
    private val staging = File(root, "staging")
    private val packages = File(root, "packages")

    private val _operationState = MutableStateFlow<ResourceOperationState>(ResourceOperationState.Idle)
    val operationState: StateFlow<ResourceOperationState> = _operationState.asStateFlow()
    private val _registry = MutableStateFlow<ResourceRegistry?>(null)
    val registry: StateFlow<ResourceRegistry?> = _registry.asStateFlow()

    fun clearOperationState() {
        if (_operationState.value is ResourceOperationState.Complete || _operationState.value is ResourceOperationState.Failed) {
            _operationState.value = ResourceOperationState.Idle
        }
    }

    suspend fun refreshRegistry(registryUrl: String): Result<ResourceRegistry> = withContext(Dispatchers.IO) {
        runCatching {
            downloads.mkdirs()
            val cache = File(root, "registry.json")
            val etagFile = File(root, "registry.etag")
            val connection = (URL(registryUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                if (etagFile.isFile) setRequestProperty("If-None-Match", etagFile.readText())
            }
            connection.connect()
            val content = when (connection.responseCode) {
                HttpURLConnection.HTTP_NOT_MODIFIED -> {
                    require(cache.isFile) { "本地没有可用的资源注册表" }
                    cache.readText()
                }
                in 200..299 -> connection.inputStream.bufferedReader().use { it.readText() }.also { text ->
                    cache.writeText(text)
                    connection.getHeaderField("ETag")?.let { etagFile.writeText(it) }
                }
                else -> error("资源注册表请求失败：HTTP ${connection.responseCode}")
            }
            connection.disconnect()
            val registry = json.decodeFromString<ResourceRegistry>(content)
            validateRegistry(registry)
            registry.also { _registry.value = it }
        }
    }

    suspend fun downloadAndInstall(
        item: ResourceRegistryPackage,
    ): Result<ResourceManifest> = withContext(Dispatchers.IO) {
        runCatching {
            _operationState.value = ResourceOperationState.Downloading(item.packageId, 0L, null)
            downloads.mkdirs()
            val partial = File(downloads, "${safeName(item.packageId)}-${item.versionCode}.part")
            val failures = mutableListOf<String>()
            var downloaded = false
            for (source in item.downloadUrls) {
                val attempt = runCatching { download(source, partial, item.packageId) }
                if (attempt.isSuccess) {
                    downloaded = true
                    break
                }
                failures += attempt.exceptionOrNull()?.message ?: source
            }
            require(downloaded && partial.isFile && partial.length() > 0L) {
                "资源下载失败：${failures.joinToString("；")}" 
            }
            installArchive(
                archive = partial,
                expectedSha256 = item.sha256,
                originUrl = item.sourceRepository.ifBlank { item.releasePageUrl },
            )
        }.onFailure { error ->
            _operationState.value = ResourceOperationState.Failed(item.packageId, error.message ?: "资源下载失败")
        }
    }

    suspend fun downloadAndInstallFromUrl(rawUrl: String): Result<ResourceManifest> = withContext(Dispatchers.IO) {
        runCatching {
            _operationState.value = ResourceOperationState.Downloading("URL 资源", 0L, null)
            val target = resolveResourceUrl(rawUrl)
            downloads.mkdirs()
            val partial = File(downloads, "url-${UUID.randomUUID()}.part")
            download(target.downloadUrl, partial, "URL 资源")
            installArchive(
                archive = partial,
                expectedSha256 = target.expectedSha256,
                originUrl = target.originUrl,
            )
        }.onFailure { error ->
            _operationState.value = ResourceOperationState.Failed(null, error.message ?: "URL 资源添加失败")
        }
    }

    suspend fun deleteDownloadedResource(resource: com.ilunyu.lunyu.data.db.InstalledResourceEntity): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(resource.locationType == ResourceLocationType.DOWNLOADED) { "内置资源不能删除" }
                val packageRoot = File(packages, safeName(resource.packageId)).canonicalFile
                val target = File(resource.rootPath).canonicalFile
                require(target.path.startsWith(packageRoot.path + File.separator)) { "资源目录不合法" }
                if (packageRoot.exists()) {
                    require(packageRoot.deleteRecursively()) { "资源文件删除失败" }
                }
                resourceRepository.removeDownloadedPackage(resource.packageId)
                _operationState.value = ResourceOperationState.Idle
            }.onFailure { error ->
                _operationState.value = ResourceOperationState.Failed(resource.packageId, error.message ?: "资源删除失败")
            }
        }

    private fun download(urlString: String, partial: File, packageId: String) {
        var connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            requestMethod = "GET"
            val existing = partial.length()
            if (existing > 0L) setRequestProperty("Range", "bytes=$existing-")
        }
        connection.connect()
        val append = partial.exists() && partial.length() > 0L && connection.responseCode == HttpURLConnection.HTTP_PARTIAL
        if (!append && partial.exists()) partial.delete()
        if (connection.responseCode !in 200..299) {
            connection.disconnect()
            error("下载请求失败：HTTP ${connection.responseCode}")
        }
        val previousBytes = if (append) partial.length() else 0L
        val responseBytes = connection.contentLengthLong.takeIf { it >= 0L }
        val totalBytes = responseBytes?.let { it + previousBytes }
        connection.inputStream.use { input ->
            FileOutputStream(partial, append).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var received = previousBytes
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                    received += count
                    _operationState.value = ResourceOperationState.Downloading(packageId, received, totalBytes)
                }
            }
        }
        connection.disconnect()
    }

    private suspend fun installArchive(
        archive: File,
        expectedSha256: String?,
        originUrl: String,
    ): ResourceManifest {
        val archiveSha256 = sha256(archive)
        if (expectedSha256 != null && !hashesMatch(expectedSha256, archiveSha256)) {
            error("资源包校验失败")
        }
        val operationId = UUID.randomUUID().toString()
        val targetStaging = File(staging, operationId)
        try {
            _operationState.value = ResourceOperationState.Installing(archive.name)
            unpackArchive(archive, targetStaging)
            val manifest = readAndValidateManifest(targetStaging)
            _operationState.value = ResourceOperationState.Installing(manifest.packageId)
            val target = File(File(packages, safeName(manifest.packageId)), manifest.versionCode.toString())
            target.parentFile?.mkdirs()
            val packageDirectory = if (target.exists()) {
                targetStaging.deleteRecursively()
                target
            } else {
                check(targetStaging.renameTo(target)) { "资源包安装目录切换失败" }
                target
            }
            resourceRepository.registerDownloadedPackage(manifest, packageDirectory, archiveSha256, originUrl)
            archive.delete()
            _operationState.value = ResourceOperationState.Complete(manifest.packageId, manifest.versionName)
            return manifest
        } catch (error: Throwable) {
            targetStaging.deleteRecursively()
            throw error
        }
    }

    private fun resolveResourceUrl(rawUrl: String): ResolvedDownload {
        val url = rawUrl.trim()
        require(url.startsWith("https://")) { "请输入 HTTPS 资源链接" }
        val githubRelease = GITHUB_RELEASE_URL.matchEntire(url)
        if (githubRelease != null) {
            return resolveGithubRelease(githubRelease.groupValues[1], githubRelease.groupValues[2], githubRelease.groupValues[3], url)
        }
        val githubLatestRelease = GITHUB_LATEST_RELEASE_URL.matchEntire(url)
        if (githubLatestRelease != null) {
            return resolveGithubReleaseApi(
                "https://api.github.com/repos/${githubLatestRelease.groupValues[1]}/${githubLatestRelease.groupValues[2]}/releases/latest",
                url,
            )
        }
        val githubRepository = GITHUB_REPOSITORY_URL.matchEntire(url)
        if (githubRepository != null) {
            val owner = githubRepository.groupValues[1]
            val repository = githubRepository.groupValues[2]
            return resolveGithubReleaseApi(
                "https://api.github.com/repos/$owner/$repository/releases/latest",
                url,
            )
        }
        return ResolvedDownload(downloadUrl = url, expectedSha256 = null, originUrl = url)
    }

    private fun resolveGithubRelease(owner: String, repository: String, tag: String, originUrl: String): ResolvedDownload {
        val encodedTag = URLEncoder.encode(tag, Charsets.UTF_8.name())
        return resolveGithubReleaseApi(
            "https://api.github.com/repos/$owner/$repository/releases/tags/$encodedTag",
            originUrl,
        )
    }

    private fun resolveGithubReleaseApi(apiUrl: String, originUrl: String): ResolvedDownload {
        val release = json.decodeFromString<GithubRelease>(readText(apiUrl))
        val archive = release.assets.firstOrNull { it.name == "resource.ilunyupack" }
            ?: release.assets.firstOrNull { it.name.endsWith(".ilunyupack") }
            ?: error("该 GitHub Release 未提供 .ilunyupack 资源包")
        val metadata = release.assets.firstOrNull { it.name == "release.json" }?.let { asset ->
            runCatching {
                json.decodeFromString<ResourceReleaseMetadata>(readText(asset.downloadUrl))
            }.getOrNull()
        }
        return ResolvedDownload(
            downloadUrl = archive.downloadUrl,
            expectedSha256 = metadata?.sha256?.takeIf { it.length == SHA256_HEX_LENGTH },
            originUrl = originUrl,
        )
    }

    private fun readText(urlString: String): String {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        return try {
            connection.connect()
            require(connection.responseCode in 200..299) {
                "链接请求失败：HTTP ${connection.responseCode}"
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun unpackArchive(archive: File, destination: File) {
        destination.mkdirs()
        val rootPath = destination.canonicalPath + File.separator
        var fileCount = 0
        var expandedBytes = 0L
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                require(name.isNotBlank() && !name.startsWith('/') && !name.contains("\\")) { "资源包路径不合法" }
                val output = File(destination, name).canonicalFile
                require(output.path.startsWith(rootPath)) { "资源包包含越界路径" }
                if (entry.isDirectory) {
                    output.mkdirs()
                    zip.closeEntry()
                    continue
                }
                fileCount += 1
                require(fileCount <= MAX_FILE_COUNT) { "资源包文件数量超过限制" }
                output.parentFile?.mkdirs()
                output.outputStream().use { stream ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        expandedBytes += count
                        require(expandedBytes <= MAX_EXPANDED_BYTES) { "资源包展开后的大小超过限制" }
                        stream.write(buffer, 0, count)
                    }
                }
                zip.closeEntry()
            }
        }
        require(expandedBytes <= archive.length() * MAX_COMPRESSION_RATIO + MAX_COMPRESSION_SLACK) {
            "资源包压缩比超过限制"
        }
    }

    private fun readAndValidateManifest(packageDirectory: File): ResourceManifest {
        val manifestFile = File(packageDirectory, "manifest.json")
        require(manifestFile.isFile) { "资源包缺少 manifest.json" }
        val manifest = json.decodeFromString<ResourceManifest>(manifestFile.readText())
        require(manifest.packageFormat == PACKAGE_FORMAT) { "资源包格式版本不受支持" }
        require(manifest.contentSchema == CONTENT_SCHEMA) { "资源内容版本不受支持" }
        require(PACKAGE_ID.matches(manifest.packageId)) { "资源包标识不合法" }
        require(manifest.kind == ResourceKind.EDITION || manifest.kind == ResourceKind.EXERCISE) { "资源包类型不受支持" }
        require(manifest.versionCode > 0 && manifest.minAppVersionCode <= APP_VERSION_CODE) { "资源包版本不兼容" }
        require(manifest.files.isNotEmpty()) { "资源包未声明内容文件" }
        manifest.files.forEach { (path, expectedHash) ->
            require(path.startsWith("content/") && !path.contains("..")) { "资源清单路径不合法" }
            val file = File(packageDirectory, path).canonicalFile
            require(file.isFile && file.path.startsWith(packageDirectory.canonicalPath + File.separator)) { "资源清单文件缺失" }
            require(hashesMatch(expectedHash, sha256(file))) { "资源文件校验失败：$path" }
        }
        return manifest
    }

    private fun validateRegistry(registry: ResourceRegistry) {
        require(registry.schemaVersion == REGISTRY_SCHEMA_VERSION) { "资源注册表版本不受支持" }
        registry.packages.forEach { item ->
            require(PACKAGE_ID.matches(item.packageId)) { "资源注册表包含不合法的资源标识" }
            require(item.kind == ResourceKind.EDITION || item.kind == ResourceKind.EXERCISE) {
                "资源注册表包含不支持的资源类型"
            }
            require(item.versionCode > 0 && item.size > 0 && item.sha256.length == SHA256_HEX_LENGTH) {
                "资源注册表中的资源版本信息不完整"
            }
            require(item.downloadUrls.isNotEmpty() && item.downloadUrls.all { it.startsWith("https://") }) {
                "资源注册表中的下载地址不合法"
            }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun hashesMatch(expected: String, actual: String): Boolean {
        return expected.removePrefix("sha256:").equals(actual, ignoreCase = true)
    }

    private fun safeName(value: String): String = value.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private companion object {
        const val PACKAGE_FORMAT = 1
        const val CONTENT_SCHEMA = 1
        const val APP_VERSION_CODE = 1
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000
        const val MAX_FILE_COUNT = 2_000
        const val MAX_EXPANDED_BYTES = 64L * 1024L * 1024L
        const val MAX_COMPRESSION_RATIO = 200L
        const val MAX_COMPRESSION_SLACK = 1L * 1024L * 1024L
        const val REGISTRY_SCHEMA_VERSION = 1
        const val SHA256_HEX_LENGTH = 64
        val PACKAGE_ID = Regex("^[a-z][a-z0-9._-]{2,127}$")
        val GITHUB_RELEASE_URL = Regex("https://github\\.com/([^/]+)/([^/]+)/releases/tag/([^/?#]+)/*(?:[?#].*)?")
        val GITHUB_LATEST_RELEASE_URL = Regex("https://github\\.com/([^/]+)/([^/]+)/releases(?:/latest)?/*(?:[?#].*)?")
        val GITHUB_REPOSITORY_URL = Regex("https://github\\.com/([^/]+)/([^/?#]+)/?(?:[?#].*)?")
    }
}

private data class ResolvedDownload(
    val downloadUrl: String,
    val expectedSha256: String?,
    val originUrl: String,
)
