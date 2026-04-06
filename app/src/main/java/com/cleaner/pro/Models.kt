package com.cleaner.pro

import android.graphics.drawable.Drawable
import kotlin.math.log10
import kotlin.math.pow

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable?,
    val cacheSize: Long,
    val dataSize: Long,
    val totalSize: Long,
    val lastUsed: Long,
    val isSystemApp: Boolean,
    var isSelected: Boolean = false
)

data class CleanItem(
    val category: String,
    val title: String,
    val packageName: String,
    val icon: Drawable?,
    val size: Long,
    var isSelected: Boolean = true
)

data class CleanCategory(
    val name: String,
    val icon: String,
    val totalSize: Long,
    val items: MutableList<CleanItem> = mutableListOf(),
    var isExpanded: Boolean = false,
    var isSelected: Boolean = true
)

data class MediaItem(
    val id: Long,
    val uri: android.net.Uri,
    val name: String,
    val size: Long,
    val mimeType: String
)

data class MediaInfo(
    val photoCount: Int,
    val videoCount: Int,
    val audioCount: Int,
    val photoSize: Long,
    val videoSize: Long,
    val audioSize: Long
)

data class StorageInfo(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val cleanableBytes: Long
)

fun Long.toReadableSize(): String {
    if (this <= 0) return "0 B"
    val units = arrayOf("B", "kB", "MB", "GB")
    val i = (log10(this.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val v = this / 1024.0.pow(i.toDouble())
    return if (v >= 100) "%.0f %s".format(v, units[i]) else "%.1f %s".format(v, units[i])
}
