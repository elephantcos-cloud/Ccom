package com.cleaner.pro

import android.content.ContentUris
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.io.File

class MediaActivity : BaseActivity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    private val items = mutableListOf<MediaItem>()
    private lateinit var adapter: MediaGridAdapter
    private lateinit var countTv: TextView
    private var currentTab = 0  // 0=ছবি 1=ভিডিও 2=অডিও 3=সব ফাইল
    private var isSelectionMode = false

    // Bottom action buttons
    private lateinit var shareBtn: TextView
    private lateinit var backupBtn: TextView
    private lateinit var deleteBtn: TextView

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        ThemeHelper.applyTheme(this)
        window.statusBarColor = ThemeHelper.bgColor(this)
        supportActionBar?.hide()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeHelper.bgColor(this@MediaActivity))
        }

        // Top bar
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(44), dp(16), dp(12))
            setBackgroundColor(ThemeHelper.navColor(this@MediaActivity))
        }
        top.addView(TextView(this).apply {
            text = "←"; textSize = 22f
            setTextColor(ThemeHelper.textPrimary(this@MediaActivity))
            setPadding(0, 0, dp(16), 0)
            setOnClickListener { finish() }
        })
        top.addView(TextView(this).apply {
            text = getString(R.string.media_overview); textSize = 18f
            setTextColor(ThemeHelper.textPrimary(this@MediaActivity))
            typeface = Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(top)

        // Tab row: ছবি | ভিডিও | অডিও | ফাইল
        root.addView(buildTabRow())

        countTv = TextView(this).apply {
            textSize = 12f
            setTextColor(ThemeHelper.textSecondary(this@MediaActivity))
            setPadding(dp(16), dp(6), dp(16), dp(4))
        }
        root.addView(countTv)

        // Grid RecyclerView
        val rv = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(context, 3)
        }
        adapter = MediaGridAdapter(
            items = items,
            onItemClick = { item -> openItem(item) },
            onItemLongClick = { item -> toggleSelection(item) },
            onSelectionChanged = { updateBottomBar() }
        )
        rv.adapter = adapter
        root.addView(rv, LinearLayout.LayoutParams(-1, 0, 1f))

        // Bottom bar
        root.addView(buildBottomBar())
        setContentView(root)
        loadItems()
    }

    private fun buildTabRow(): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(6), dp(16), dp(6))
            setBackgroundColor(ThemeHelper.navColor(this@MediaActivity))
        }
        val tabs = listOf("ছবি", "ভিডিও", "অডিও", "ফাইল")
        tabs.forEachIndexed { i, label ->
            val active = (i == currentTab)
            row.addView(TextView(this).apply {
                text = label; textSize = 13f; gravity = Gravity.CENTER
                setPadding(dp(12), dp(7), dp(12), dp(7))
                setTextColor(if (active) Color.WHITE else ThemeHelper.textSecondary(this@MediaActivity))
                typeface = if (active) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                background = if (active) GradientDrawable().apply {
                    setColor(Color.parseColor("#4A90E2"))
                    cornerRadius = dp(20).toFloat()
                } else null
                setOnClickListener {
                    if (currentTab != i) {
                        currentTab = i
                        // Rebuild tab row
                        val parent = (this.parent as? LinearLayout)
                        val parentRoot = parent?.parent as? LinearLayout
                        if (parent != null && parentRoot != null) {
                            val idx = (0 until parentRoot.childCount).firstOrNull {
                                parentRoot.getChildAt(it) == parent
                            } ?: return@setOnClickListener
                            parentRoot.removeView(parent)
                            parentRoot.addView(buildTabRow(), idx)
                        }
                        clearSelection()
                        loadItems()
                    }
                }
            }, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(6) })
        }
        return row
    }

    private fun buildBottomBar(): LinearLayout {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(12), dp(8), dp(24))
            setBackgroundColor(ThemeHelper.navColor(this@MediaActivity))
        }

        shareBtn = actionBtn("SHARE", Color.parseColor("#4A90E2")) { doShare() }
        backupBtn = actionBtn("BACKUP", Color.parseColor("#4A90E2")) { doBackup() }
        deleteBtn = actionBtn("DELETE", Color.parseColor("#E23A3A")) { doDelete() }

        bar.addView(shareBtn, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(4), 0, dp(4), 0) })
        bar.addView(backupBtn, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(4), 0, dp(4), 0) })
        bar.addView(deleteBtn, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(4), 0, dp(4), 0) })

        return bar
    }

    private fun actionBtn(text: String, color: Int, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(10), 0, dp(10))
            background = GradientDrawable().apply {
                setColor(color)
                cornerRadius = dp(10).toFloat()
            }
            setOnClickListener { onClick() }
        }
    }

    private fun loadItems() {
        CoroutineScope(Dispatchers.IO).launch {
            val loaded = when (currentTab) {
                0 -> StorageHelper.queryMedia(this@MediaActivity, 0)
                1 -> StorageHelper.queryMedia(this@MediaActivity, 1)
                2 -> StorageHelper.queryMedia(this@MediaActivity, 2)
                else -> scanAllFiles()
            }
            withContext(Dispatchers.Main) {
                items.clear()
                items.addAll(loaded)
                adapter.notifyDataSetChanged()
                val total = items.sumOf { it.size }
                countTv.text = "${items.size} TOTAL (${total.toReadableSize()})"
            }
        }
    }

    // ✅ সব ধরনের ফাইল scan করা
    private fun scanAllFiles(): List<MediaItem> {
        val result = mutableListOf<MediaItem>()
        val allowedExtensions = setOf(
            "pdf", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "html", "htm", "xml", "json", "csv", "zip", "rar", "7z",
            "apk", "mp4", "mp3", "jpg", "jpeg", "png", "gif", "mkv",
            "avi", "mov", "wav", "flac", "ogg", "epub", "mobi"
        )
        try {
            fun scan(dir: File, depth: Int = 0) {
                if (depth > 5) return
                dir.listFiles()?.forEach { f ->
                    if (f.isDirectory) scan(f, depth + 1)
                    else {
                        val ext = f.extension.lowercase()
                        if (ext in allowedExtensions || depth < 2) {
                            val mimeType = getMimeType(ext)
                            result.add(MediaItem(
                                id = f.hashCode().toLong(),
                                uri = Uri.fromFile(f),
                                name = f.name,
                                size = f.length(),
                                mimeType = mimeType
                            ))
                        }
                    }
                }
            }
            scan(Environment.getExternalStorageDirectory())
        } catch (_: Exception) {}
        return result.sortedByDescending { it.size }
    }

    private fun getMimeType(ext: String): String = when (ext) {
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        "html", "htm" -> "text/html"
        "xml" -> "text/xml"
        "json" -> "application/json"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "zip" -> "application/zip"
        "rar" -> "application/x-rar-compressed"
        "apk" -> "application/vnd.android.package-archive"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "mp4" -> "video/mp4"
        "mkv" -> "video/x-matroska"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "flac" -> "audio/flac"
        else -> "*/*"
    }

    // ✅ Fix: ফাইল Open করা
    private fun openItem(item: MediaItem) {
        if (isSelectionMode) {
            toggleSelection(item)
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(item.uri, item.mimeType.ifEmpty { "*/*" })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            // uri দিয়ে না হলে file path দিয়ে try করো
            try {
                val file = File(item.uri.path ?: return)
                val uri = FileProvider.getUriForFile(
                    this, "$packageName.provider", file
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, item.mimeType.ifEmpty { "*/*" })
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Open with"))
            } catch (_: Exception) {
                Toast.makeText(this, "এই ফাইল খোলার জন্য app নেই", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleSelection(item: MediaItem) {
        isSelectionMode = true
        item.isSelected = !item.isSelected
        if (items.none { it.isSelected }) isSelectionMode = false
        adapter.notifyDataSetChanged()
        updateBottomBar()
    }

    private fun clearSelection() {
        isSelectionMode = false
        items.forEach { it.isSelected = false }
        adapter.notifyDataSetChanged()
        updateBottomBar()
    }

    private fun updateBottomBar() {
        val selCount = items.count { it.isSelected }
        countTv.text = if (selCount > 0)
            "$selCount selected — ${items.filter { it.isSelected }.sumOf { it.size }.toReadableSize()}"
        else "${items.size} TOTAL (${items.sumOf { it.size }.toReadableSize()})"
    }

    // ✅ Fix: Share করা
    private fun doShare() {
        val selected = items.filter { it.isSelected }
        if (selected.isEmpty()) {
            Toast.makeText(this, "কোনো ফাইল select করা হয়নি", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val uris = ArrayList(selected.map { it.uri })
            val intent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = selected.first().mimeType.ifEmpty { "*/*" }
                    putExtra(Intent.EXTRA_STREAM, uris.first())
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "*/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            startActivity(Intent.createChooser(intent, "Share via"))
        } catch (_: Exception) {
            Toast.makeText(this, "Share করতে পারছে না", Toast.LENGTH_SHORT).show()
        }
    }

    // ✅ Fix: Backup করা (Download/CleanerPro_Backup ফোল্ডারে copy)
    private fun doBackup() {
        val selected = items.filter { it.isSelected }
        if (selected.isEmpty()) {
            Toast.makeText(this, "কোনো ফাইল select করা হয়নি", Toast.LENGTH_SHORT).show()
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            var success = 0
            val backupDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "CleanerPro_Backup"
            )
            backupDir.mkdirs()
            selected.forEach { item ->
                try {
                    val src = contentResolver.openInputStream(item.uri) ?: return@forEach
                    val dest = File(backupDir, item.name)
                    dest.outputStream().use { out -> src.copyTo(out) }
                    src.close()
                    success++
                } catch (_: Exception) {}
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@MediaActivity,
                    "$success টি ফাইল Backup হয়েছে → Downloads/CleanerPro_Backup",
                    Toast.LENGTH_LONG
                ).show()
                clearSelection()
            }
        }
    }

    // ✅ Fix: Delete করা (confirm dialog সহ)
    private fun doDelete() {
        val selected = items.filter { it.isSelected }
        if (selected.isEmpty()) {
            Toast.makeText(this, "কোনো ফাইল select করা হয়নি", Toast.LENGTH_SHORT).show()
            return
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("Delete করবেন?")
            .setMessage("${selected.size} টি ফাইল মুছে যাবে। এটা undo করা যাবে না।")
            .setPositiveButton("Delete") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    var deleted = 0
                    selected.forEach { item ->
                        try {
                            val rows = contentResolver.delete(item.uri, null, null)
                            if (rows > 0) deleted++
                        } catch (_: Exception) {
                            try {
                                val path = item.uri.path
                                if (path != null && File(path).delete()) deleted++
                            } catch (_: Exception) {}
                        }
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MediaActivity,
                            "$deleted টি ফাইল delete হয়েছে",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadItems()
                        clearSelection()
                    }
                }
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }
}

// MediaItem এ isSelected field যোগ করো (Models.kt এ)
var MediaItem.isSelected: Boolean
    get() = _selectedItems.contains(id)
    set(value) {
        if (value) _selectedItems.add(id) else _selectedItems.remove(id)
    }

private val _selectedItems = mutableSetOf<Long>()

class MediaGridAdapter(
    private val items: List<MediaItem>,
    private val onItemClick: (MediaItem) -> Unit,
    private val onItemLongClick: (MediaItem) -> Unit,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private fun dp(v: Int) = (v * android.content.res.Resources.getSystem().displayMetrics.density + 0.5f).toInt()

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val size = parent.width / 3
        val frame = android.widget.FrameLayout(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(size, size)
        }
        return object : RecyclerView.ViewHolder(frame) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
        val item = items[pos]
        val ctx = holder.itemView.context
        val frame = holder.itemView as android.widget.FrameLayout
        frame.removeAllViews()

        val size = frame.layoutParams.width

        // Thumbnail
        val img = ImageView(ctx).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(-1, -1)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.parseColor("#1A2A44"))
        }

        // ✅ Thumbnail load করো
        if (item.mimeType.startsWith("image")) {
            try {
                val bmp = android.provider.MediaStore.Images.Thumbnails.getThumbnail(
                    ctx.contentResolver, item.id,
                    android.provider.MediaStore.Images.Thumbnails.MINI_KIND, null
                )
                if (bmp != null) img.setImageBitmap(bmp)
                else img.setImageResource(R.drawable.ic_media)
            } catch (_: Exception) { img.setImageResource(R.drawable.ic_media) }
        } else if (item.mimeType.startsWith("video")) {
            try {
                val bmp = android.provider.MediaStore.Video.Thumbnails.getThumbnail(
                    ctx.contentResolver, item.id,
                    android.provider.MediaStore.Video.Thumbnails.MINI_KIND, null
                )
                if (bmp != null) img.setImageBitmap(bmp)
                else img.setImageResource(R.drawable.ic_media)
            } catch (_: Exception) { img.setImageResource(R.drawable.ic_media) }
        } else {
            img.setImageResource(R.drawable.ic_media)
        }

        frame.addView(img)

        // Selection overlay
        if (item.isSelected) {
            val overlay = View(ctx).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(-1, -1)
                setBackgroundColor(Color.parseColor("#804A90E2"))
            }
            frame.addView(overlay)
            val check = TextView(ctx).apply {
                text = "✓"
                textSize = 18f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            frame.addView(check)
        }

        // File size label
        val sizeTv = TextView(ctx).apply {
            text = item.size.toReadableSize()
            textSize = 10f
            setTextColor(Color.WHITE)
            setPadding(dp(4), dp(2), dp(4), dp(4))
            setShadowLayer(2f, 0f, 1f, Color.BLACK)
            gravity = Gravity.BOTTOM or Gravity.START
            layoutParams = android.widget.FrameLayout.LayoutParams(-1, -1)
        }
        frame.addView(sizeTv)

        // Click handlers
        frame.setOnClickListener { onItemClick(item); onSelectionChanged() }
        frame.setOnLongClickListener {
            onItemLongClick(item)
            onSelectionChanged()
            true
        }

        // Border padding
        frame.setPadding(dp(1), dp(1), dp(1), dp(1))
    }
}
