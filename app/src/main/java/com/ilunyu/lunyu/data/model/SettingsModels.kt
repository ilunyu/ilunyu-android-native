package com.ilunyu.lunyu.data.model

enum class AppThemeMode(val key: String, val label: String) {
    SYSTEM("system", "跟随设备"),
    LIGHT("light", "浅色模式"),
    DARK("dark", "深色模式");

    companion object {
        fun fromKey(key: String?): AppThemeMode = entries.find { it.key == key } ?: SYSTEM
    }
}

enum class AppFontPreference(val key: String, val label: String, val fontFileName: String?) {
    SANS("sans", "思源黑体", "NotoSansCJKsc-Regular.otf"),
    SERIF("serif", "思源宋体", "NotoSerifCJKsc-Regular.otf"),
    SYSTEM("system", "系统字体", null);

    companion object {
        fun fromKey(key: String?): AppFontPreference = entries.find { it.key == key } ?: SANS
    }
}
