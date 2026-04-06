package com.cleaner.pro

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import kotlinx.coroutines.*

class DashboardView(private val ctx: Context) : ScrollView(ctx) {

    private fun dp(v: Int) = (v * ctx.resources.displayMetrics.density + 0.5f).toInt()

    private lateinit var freeSpaceTv: TextView
    private lateinit var cleanableTv: TextView
    private lateinit var storageBar: StorageBarView
    private lateinit var cacheSizeTv: TextView

    init {
        isFillViewport = true
        setBackgroundColor(ThemeHelper.bgColor(ctx))
        build()
        load()
    }

    private fun build() {
        val card    = ThemeHelper.cardColor(ctx)
        val textP   = ThemeHelper.textPrimary(ctx)
        val textS   = ThemeHelper.textSecondary(ctx)
        val divider = ThemeHelper.dividerColor(ctx)

        val ll = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, dp(14), dp(24))
        }

        // Top bar
        val top = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(44), dp(2), dp(14))
        }
        val menuBtn = ImageView(ctx).apply {
            setImageResource(R.drawable.ic_menu)
            imageTintList = ColorStateList.valueOf(textP)
            setPadding(dp(8), dp(8), dp(14), dp(8))
            setOnClickListener { (ctx as? MainActivity)?.openDrawer() }
        }
        top.addView(menuBtn, LinearLayout.LayoutParams(dp(44), dp(44)))
        top.addView(TextView(ctx).apply {
            text = ctx.getString(R.string.app_name)
            textSize = 19f
            setTextColor(textP)
            typeface = Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, -2, 1f))
        val permBtn = ImageView(ctx).apply {
            setImageResource(R.drawable.ic_settings)
            imageTintList = ColorStateList.valueOf(textS)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setOnClickListener {
                ctx.startActivity(Intent(ctx, PermissionsActivity::class.java))
            }
        }
        top.addView(permBtn, LinearLayout.LayoutParams(dp(40), dp(40)))
        ll.addView(top)

        // Storage card
        val storCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = GradientDrawable().apply {
                setColor(card)
                cornerRadius = dp(20).toFloat()
                setStroke(1, divider)
            }
            elevation = dp(3).toFloat()
        }

        storCard.addView(TextView(ctx).apply {
            text = ctx.getString(R.string.free_space)
            textSize = 13f
            setTextColor(textS)
            gravity = Gravity.CENTER
        }, lp(-1, -2, 0, 0, 0, dp(4)))

        freeSpaceTv = TextView(ctx).apply {
            text = "—"
            textSize = 42f
            setTextColor(textP)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        storCard.addView(freeSpaceTv, lp(-1, -2, 0, 0, 0, dp(4)))

        cleanableTv = TextView(ctx).apply {
            text = "${ctx.getString(R.string.free_up_to)} —"
            textSize = 13f
            setTextColor(textS)
            gravity = Gravity.CENTER
        }
        storCard.addView(cleanableTv, lp(-1, -2, 0, 0, 0, dp(18)))

        storageBar = StorageBarView(ctx)
        storCard.addView(storageBar, LinearLayout.LayoutParams(-1, dp(18)).apply {
            setMargins(0, 0, 0, dp(18))
        })

        val legend = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        legend.addLegendRow("●", Color.parseColor("#5B9CF6"), "Unneeded files", "—")
        cacheSizeTv = legend.addLegendRow("●", Color.parseColor("#F5A623"),
            ctx.getString(R.string.hidden_caches), "—")
        legend.addLegendRow("●", Color.parseColor("#7ED321"), "Files to review", "0 B")
        storCard.addView(legend, lp(-1, -2, 0, 0, 0, dp(18)))

        storCard.addView(View(ctx).apply { setBackgroundColor(divider) },
            LinearLayout.LayoutParams(-1, 1))

        storCard.addView(TextView(ctx).apply {
            text = ctx.getString(R.string.quick_clean).uppercase()
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(16), 0, dp(16))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#4A90E2"))
                cornerRadius = dp(50).toFloat()
            }
            setOnClickListener {
                ctx.startActivity(Intent(ctx, QuickCleanActivity::class.java))
            }
        }, lp(-1, -2, 0, dp(18), 0, 0))

        ll.addView(storCard, lp(-1, -2, 0, 0, 0, dp(16)))

        // Feature grid
        val isDark  = ThemeHelper.isDark(ctx)
        val grid = GridLayout(ctx).apply { columnCount = 2 }
        val gridBg = if (isDark) Color.parseColor("#111E33") else Color.parseColor("#FFFFFF")
        addFeatureCard(grid, R.drawable.ic_sleep, ctx.getString(R.string.sleep_mode), gridBg, divider) {
            ctx.startActivity(Intent(ctx, AppManagerActivity::class.java))
        }
        addFeatureCard(grid, R.drawable.ic_tips, ctx.getString(R.string.tips), gridBg, divider) {
            ctx.startActivity(Intent(ctx, PermissionsActivity::class.java))
        }
        addFeatureCard(grid, R.drawable.ic_media, ctx.getString(R.string.media), gridBg, divider) {
            ctx.startActivity(Intent(ctx, MediaActivity::class.java))
        }
        addFeatureCard(grid, R.drawable.ic_apps, ctx.getString(R.string.apps), gridBg, divider) {
            ctx.startActivity(Intent(ctx, AppManagerActivity::class.java))
        }
        ll.addView(grid)
        addView(ll)
    }

    private fun LinearLayout.addLegendRow(
        dot: String, color: Int, label: String, value: String
    ): TextView {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(5), 0, dp(5))
        }
        row.addView(TextView(ctx).apply { text = dot; setTextColor(color); textSize = 18f })
        row.addView(TextView(ctx).apply {
            text = " $label"
            textSize = 13f
            setTextColor(ThemeHelper.textPrimary(ctx))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        val valTv = TextView(ctx).apply {
            text = value
            textSize = 13f
            setTextColor(ThemeHelper.textSecondary(ctx))
        }
        row.addView(valTv)
        addView(row)
        return valTv
    }

    private fun addFeatureCard(
        grid: GridLayout, icon: Int, label: String,
        bgColor: Int, stroke: Int, onClick: () -> Unit
    ) {
        val textCol = ThemeHelper.textPrimary(ctx)
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(22), dp(16), dp(22))
            background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = dp(18).toFloat()
                setStroke(1, stroke)
            }
            elevation = dp(2).toFloat()
            setOnClickListener { onClick() }
        }
        card.addView(ImageView(ctx).apply {
            setImageResource(icon)
            imageTintList = ColorStateList.valueOf(Color.parseColor("#4A90E2"))
        }, LinearLayout.LayoutParams(dp(34), dp(34)).apply { bottomMargin = dp(10) })
        card.addView(TextView(ctx).apply {
            text = label
            textSize = 14f
            setTextColor(textCol)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })

        val spec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        grid.addView(card, GridLayout.LayoutParams(spec, spec).apply {
            width = 0; height = -2
            setMargins(dp(5), dp(5), dp(5), dp(5))
        })
    }

    private fun load() {
        CoroutineScope(Dispatchers.IO).launch {
            val info = StorageHelper.getStorageInfo(ctx)
            withContext(Dispatchers.Main) {
                freeSpaceTv.text = info.freeBytes.toReadableSize()
                cleanableTv.text = "${ctx.getString(R.string.free_up_to)} ${info.cleanableBytes.toReadableSize()}"
                storageBar.setValues(info.usedBytes, info.totalBytes)
                cacheSizeTv.text = info.cleanableBytes.toReadableSize()
            }
        }
    }

    private fun lp(w: Int, h: Int, l: Int, t: Int, r: Int, b: Int) =
        LinearLayout.LayoutParams(w, h).apply { setMargins(l, t, r, b) }
}

class StorageBarView(ctx: Context) : View(ctx) {
    private var usedFrac = 0f
    private val bgPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1E2E4A") }
    private val freePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4A90E2") }
    private val usedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F5A623") }

    fun setValues(used: Long, total: Long) {
        usedFrac = if (total > 0) used.toFloat() / total else 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val r = height / 2f
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRoundRect(RectF(0f, 0f, w, h), r, r, bgPaint)
        val freeEnd = w * (1f - usedFrac)
        if (freeEnd > 2f) canvas.drawRoundRect(RectF(0f, 0f, freeEnd, h), r, r, freePaint)
        if (freeEnd < w - 2f) canvas.drawRoundRect(RectF(freeEnd, 0f, w, h), r, r, usedPaint)
    }
}
