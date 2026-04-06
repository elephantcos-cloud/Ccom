package com.cleaner.pro

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class MediaActivity : BaseActivity() {

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    private var currentTab = 0
    private lateinit var rv: RecyclerView
    private lateinit var countTv: TextView
    private lateinit var tabRow: LinearLayout

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        ThemeHelper.applyTheme(this)
        window.statusBarColor = ThemeHelper.bgColor(this)
        supportActionBar?.hide()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeHelper.bgColor(this@MediaActivity))
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(44), dp(16), dp(12))
            setBackgroundColor(ThemeHelper.navColor(this@MediaActivity))
        }
        top.addView(TextView(this).apply {
            text = "←"; textSize = 22f
            setTextColor(ThemeHelper.textPrimary(this@MediaActivity))
            setPadding(0, 0, dp(16), 0); setOnClickListener { finish() }
        })
        top.addView(TextView(this).apply {
            text = getString(R.string.media_overview); textSize = 18f
            setTextColor(ThemeHelper.textPrimary(this@MediaActivity))
            typeface = Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(top)

        tabRow = buildTabRow()
        root.addView(tabRow)

        countTv = TextView(this).apply {
            textSize = 12f
            setTextColor(ThemeHelper.textSecondary(this@MediaActivity))
            setPadding(dp(16), dp(8), dp(16), dp(4))
        }
        root.addView(countTv)

        rv = RecyclerView(this).apply { layoutManager = GridLayoutManager(context, 3) }
        root.addView(rv, LinearLayout.LayoutParams(-1, 0, 1f))

        root.addView(buildBottomBar())
        setContentView(root)
        loadMedia(0)
    }

    private fun buildTabRow(): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(6), dp(16), dp(6))
            setBackgroundColor(ThemeHelper.navColor(this@MediaActivity))
        }
        listOf(getString(R.string.photos), getString(R.string.videos), getString(R.string.audio))
            .forEachIndexed { i, label ->
                val btn = TextView(this).apply {
                    text = label; textSize = 13f; gravity = Gravity.CENTER
                    setPadding(dp(14), dp(8), dp(14), dp(8))
                    setTextColor(if (i == currentTab) Color.WHITE
                                 else ThemeHelper.textSecondary(this@MediaActivity))
                    typeface = if (i == currentTab) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    background = if (i == currentTab) GradientDrawable().apply {
                        setColor(Color.parseColor("#4A90E2"))
                        cornerRadius = dp(20).toFloat()
                    } else null
                    setOnClickListener {
                        currentTab = i
                        refreshTabs()
                        loadMedia(i)
                    }
                }
                row.addView(btn, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(8) })
            }
        return row
    }

    private fun refreshTabs() {
        for (i in 0 until tabRow.childCount) {
            val btn = tabRow.getChildAt(i) as? TextView ?: continue
            val active = (i == currentTab)
            btn.setTextColor(if (active) Color.WHITE else ThemeHelper.textSecondary(this))
            btn.typeface = if (active) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            btn.background = if (active) GradientDrawable().apply {
                setColor(Color.parseColor("#4A90E2"))
                cornerRadius = dp(20).toFloat()
            } else null
        }
    }

    private fun buildBottomBar(): LinearLayout {
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            setPadding(dp(16), dp(10), dp(16), dp(20))
            setBackgroundColor(ThemeHelper.navColor(this@MediaActivity))
        }
        listOf(
            Triple(R.drawable.ic_media, "SHARE",  "#4A90E2"),
            Triple(R.drawable.ic_tips,  "BACKUP", "#4A90E2"),
            Triple(R.drawable.ic_apps,  "DELETE", "#E53935")
        ).forEach { (icon, label, col) ->
            val btn = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                setPadding(0, dp(6), 0, dp(6))
            }
            btn.addView(ImageView(this).apply {
                setImageResource(icon)
                imageTintList = ColorStateList.valueOf(Color.parseColor(col))
            }, LinearLayout.LayoutParams(dp(22), dp(22)))
            btn.addView(TextView(this).apply {
                text = label; textSize = 9f
                setTextColor(Color.parseColor(col)); gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
            })
            bottom.addView(btn, LinearLayout.LayoutParams(0, -2, 1f))
        }
        return bottom
    }

    private fun loadMedia(type: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val items = StorageHelper.queryMedia(this@MediaActivity, type)
            withContext(Dispatchers.Main) {
                countTv.text = "${items.size} TOTAL (${items.sumOf { it.size }.toReadableSize()})"
                rv.adapter = MediaGridAdapter(items) { item -> openItem(item) }
            }
        }
    }

    private fun openItem(item: MediaItem) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(item.uri, item.mimeType)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            })
        } catch (_: Exception) {
            Toast.makeText(this, "ফাইল খোলা যাচ্ছে না", Toast.LENGTH_SHORT).show()
        }
    }
}

class MediaGridAdapter(
    private val items: List<MediaItem>,
    private val onClick: (MediaItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private fun dp(v: Int) =
        (v * android.content.res.Resources.getSystem().displayMetrics.density + 0.5f).toInt()

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        object : RecyclerView.ViewHolder(LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(2), dp(2), dp(2), dp(2))
        }) {}

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
        val item = items[pos]
        val ctx  = holder.itemView.context
        val cell = holder.itemView as LinearLayout
        cell.removeAllViews()
        val size = (android.content.res.Resources.getSystem().displayMetrics.widthPixels / 3) - dp(8)

        val img = ImageView(ctx).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.parseColor("#1A2A44"))
            setOnClickListener { onClick(item) }
        }
        cell.addView(img, LinearLayout.LayoutParams(size, size))

        CoroutineScope(Dispatchers.IO).launch {
            val bmp = try {
                ctx.contentResolver.loadThumbnail(item.uri, android.util.Size(size, size), null)
            } catch (_: Exception) { null }
            withContext(Dispatchers.Main) { bmp?.let { img.setImageBitmap(it) } }
        }

        cell.addView(TextView(ctx).apply {
            text = item.size.toReadableSize(); textSize = 10f
            setTextColor(Color.parseColor("#CCCCCC")); gravity = Gravity.CENTER
            setPadding(0, dp(3), 0, 0)
        })
    }
}
