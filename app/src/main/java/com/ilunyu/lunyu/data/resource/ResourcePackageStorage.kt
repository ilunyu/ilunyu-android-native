package com.ilunyu.lunyu.data.resource

import android.content.Context
import java.io.File
import java.io.InputStream

class ResourcePackageStorage(private val context: Context) {

    fun open(resource: ResolvedPackage, relativePath: String): InputStream {
        return if (resource.locationType == ResourceLocationType.BUNDLED) {
            context.assets.open(relativePath)
        } else {
            File(resource.rootPath, relativePath).inputStream()
        }
    }

    fun exists(resource: ResolvedPackage, relativePath: String): Boolean {
        return if (resource.locationType == ResourceLocationType.BUNDLED) {
            runCatching { context.assets.open(relativePath).close() }.isSuccess
        } else {
            File(resource.rootPath, relativePath).isFile
        }
    }
}
