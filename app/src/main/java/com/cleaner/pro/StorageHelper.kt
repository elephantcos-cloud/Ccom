package com.cleaner.pro

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.ContentUris
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.provider.MediaStore
import java.io.File

object StorageHelper {

    fun getStorageInfo(ctx: Context): StorageInfo {
        return try {
            // ✅ Fix: freeBlocksLong ব্যবহার করো (Files app এর মতো)
            val path = Environment.getExternalStorageDirectory()
            val stat = StatFs(path.absolutePath)
            val blockSize = stat.blockSizeLong
            val total = blockSize * stat.blockCountLong
            // ✅ freeBlocksLong = total free (root reserved সহ) → Files app এর মতো
            val free = blockSize * stat.freeBlocksLong

            StorageInfo(total, total - free, free, estimateCleanable(ctx))
        } catch (e: Exception) {
            try {
                val stat = StatFs(Environment.getDataDirectory().absolutePath)
                val blockSize = stat.blockSizeLong
                val total = blockSize * stat.blockCountLong
                val free = blockSize * stat.freeBlocksLong
                StorageInfo(total, total - free, free, 0L)
            } catch (_: Exception) {
                StorageInfo(0, 0, 0, 0)
            }
        }
    }

    private fun estimateCleanable(ctx: Context): Long {
        var total = 0L
        try {
            val sm = ctx.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
            ctx.packageManager.getInstalledApplications(PackageManager.GET_META_DATA).forEach { app ->
                try {
                    total += sm.queryStatsForPackage(
                        StorageManager.UUID_DEFAULT,
                        app.packageName,
                        android.os.Process.myUserHandle()
                    ).cacheBytes
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        return total
    }

    fun getInstalledApps(ctx: Context, filter: String = "all"): List<AppInfo> {
        val result = mutableListOf<AppInfo>()
        val pm = ctx.packageManager
        val sm = ctx.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val usageMap = try {
            usm.queryUsageStats(
                UsageStatsManager.INTERVAL_MONTHLY,
                System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000,
                System.currentTimeMillis()
            ).associate { it.packageName to it.lastTimeUsed }
        } catch (_: Exception) { emptyMap() }

        pm.getInstalledApplications(PackageManager.GET_META_DATA).forEach { app ->
            val isSys = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (filter == "user" && isSys) return@forEach
            if (filter == "system" && !isSys) return@forEach
            try {
                val stats = sm.queryStatsForPackage(
                    StorageManager.UUID_DEFAULT,
                    app.packageName,
                    android.os.Process.myUserHandle()
                )
                result.add(AppInfo(
                    name = pm.getApplicationLabel(app).toString(),
                    packageName = app.packageName,
                    icon = try { pm.getApplicationIcon(app.packageName) } catch (_: Exception) { null },
                    cacheSize = stats.cacheBytes,
                    dataSize = stats.dataBytes,
                    totalSize = stats.appBytes + stats.dataBytes + stats.cacheBytes,
                    lastUsed = usageMap[app.packageName] ?: 0L,
                    isSystemApp = isSys
                ))
            } catch (_: Exception) {}
        }

        return result.sortedWith(
            compareByDescending<AppInfo> { it.lastUsed }.thenByDescending { it.totalSize }
        )
    }

    fun scanCleanItems(ctx: Context): List<CleanCategory> {
        val categories = mutableListOf<CleanCategory>()
        val pm = ctx.packageManager
        val sm = ctx.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager

        val cacheList = mutableListOf<CleanItem>()
        var cacheTotal = 0L

        pm.getInstalledApplications(PackageManager.GET_META_DATA).forEach { app ->
            try {
                val stats = sm.queryStatsForPackage(
                    StorageManager.UUID_DEFAULT,
                    app.packageName,
                    android.os.Process.myUserHandle()
                )
                if (stats.cacheBytes > 0) {
                    cacheList.add(CleanItem(
                        "hidden_cache",
                        pm.getApplicationLabel(app).toString(),
                        app.packageName,
                        try { pm.getApplicationIcon(app.packageName) } catch (_: Exception) { null },
                        stats.cacheBytes
                    ))
                    cacheTotal += stats.cacheBytes
                }
            } catch (_: Exception) {}
        }

        if (cacheList.isNotEmpty())
            categories.add(CleanCategory(
                "hidden_cache", "", cacheTotal,
                cacheList.sortedByDescending { it.size }.toMutableList()
            ))

        val emptyList = mutableListOf<CleanItem>()
        try { findEmptyFolders(Environment.getExternalStorageDirectory(), emptyList) } catch (_: Exception) {}
        if (emptyList.isNotEmpty())
            categories.add(CleanCategory(
                "empty_folders", "", emptyList.sumOf { it.size }, emptyList.toMutableList()
            ))

        val apkList = mutableListOf<CleanItem>()
        var apkTotal = 0L
        try {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                ?.listFiles { f -> f.name.endsWith(".apk", true) }
                ?.forEach { f ->
                    apkList.add(CleanItem("apk_files", f.name, f.absolutePath, null, f.length()))
                    apkTotal += f.length()
                }
        } catch (_: Exception) {}
        if (apkList.isNotEmpty())
            categories.add(CleanCategory("apk_files", "", apkTotal, apkList.toMutableList()))

        return categories
    }

    private fun findEmptyFolders(dir: File, result: MutableList<CleanItem>, depth: Int = 0) {
        if (depth > 3) return
        try {
            dir.listFiles()?.forEach { f ->
                if (f.isDirectory) {
                    if (f.listFiles()?.isEmpty() == true)
                        result.add(CleanItem("empty_folders", f.name, f.absolutePath, null, 4096L))
                    else
                        findEmptyFolders(f, result, depth + 1)
                }
            }
        } catch (_: Exception) {}
    }

    fun queryMedia(ctx: Context, type: Int): List<MediaItem> {
        val result = mutableListOf<MediaItem>()
        val uri = when (type) {
            0 -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            1 -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val cols = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE
        )
        try {
            ctx.contentResolver.query(
                uri, cols, null, null,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val mimeIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                while (c.moveToNext()) {
                    val id = c.getLong(idIdx)
                    val itemUri = ContentUris.withAppendedId(uri, id)
                    result.add(MediaItem(
                        id, itemUri,
                        c.getString(nameIdx) ?: "",
                        c.getLong(sizeIdx),
                        c.getString(mimeIdx) ?: ""
                    ))
                }
            }
        } catch (_: Exception) {}
        return result
    }

    fun getMediaInfo(ctx: Context): MediaInfo {
        fun q(uri: android.net.Uri): Pair<Int, Long> {
            var count = 0; var size = 0L
            try {
                ctx.contentResolver.query(
                    uri, arrayOf(MediaStore.MediaColumns.SIZE), null, null, null
                )?.use { while (it.moveToNext()) { count++; size += it.getLong(0) } }
            } catch (_: Exception) {}
            return count to size
        }
        val (pc, ps) = q(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val (vc, vs) = q(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        val (ac, as2) = q(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        return MediaInfo(pc, vc, ac, ps, vs, as2)
    }

    fun hasUsageStatsPermission(ctx: Context): Boolean = try {
        val ops = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        ops.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            ctx.packageName
        ) == AppOpsManager.MODE_ALLOWED
    } catch (_: Exception) { false }
}
