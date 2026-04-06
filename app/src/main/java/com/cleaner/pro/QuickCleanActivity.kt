package com.cleaner.pro

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class QuickCleanActivity : BaseActivity() {

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    private lateinit var statusTv: TextView
    private lateinit var rv: RecyclerView
    private lateinit var cleanBtn: TextView
    private lateinit var selectedTv: TextView
    private val cats = mutableListOf<CleanCategory>()
    private lateinit var adapter: CleanAdapter

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        ThemeHelper.applyTheme(this)
        window.statusBarColor = ThemeHelper.bgColor(this)
        supportActionBar?.hide()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeHelper.bgColor(this@QuickCleanActivity))
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(44), dp(16), dp(12))
            setBackgroundColor(ThemeHelper.navColor(this@QuickCleanActivity))
        }
        top.addView(TextView(this).apply {
            text = "←"; textSize = 22f
            setTextColor(ThemeHelper.textPrimary(this@QuickCleanActivity))
            setPadding(0, 0, dp(16), 0)
            setOnClickListener { finish() }
        })
        top.addView(TextView(this).apply {
            text = getString(R.string.quick_clean); textSize = 18f
            setTextColor(ThemeHelper.textPrimary(this@QuickCleanActivity))
            typeface = Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(top)

        statusTv = TextView(this).apply {
            text = getString(R.string.scanning); textSize = 13f
            setTextColor(ThemeHelper.textSecondary(this@QuickCleanActivity))
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(6))
        }
        root.addView(statusTv)

        rv = RecyclerView(this).apply { layoutManager = LinearLayoutManager(context) }
        adapter = CleanAdapter(cats) { updateSelInfo() }
        rv.adapter = adapter
        root.addView(rv, LinearLayout.LayoutParams(-1, 0, 1f))

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(24))
            setBackgroundColor(ThemeHelper.navColor(this@QuickCleanActivity))
        }
        selectedTv = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#4A90E2"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(8))
        }
        bottom.addView(selectedTv)
        cleanBtn = TextView(this).apply {
            text = getString(R.string.finish_cleaning).uppercase()
            textSize = 16f; setTextColor(Color.WHITE)
            gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(18), 0, dp(18))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#4A90E2"))
                cornerRadius = dp(50).toFloat()
            }
            setOnClickListener { doClean() }
        }
        bottom.addView(cleanBtn, LinearLayout.LayoutParams(-1, -2))
        root.addView(bottom)
        setContentView(root)
        scan()
    }

    private fun scan() {
        statusTv.text = getString(R.string.scanning)
        CoroutineScope(Dispatchers.IO).launch {
            val items = StorageHelper.scanCleanItems(this@QuickCleanActivity)
            withContext(Dispatchers.Main) {
                cats.clear(); cats.addAll(items)
                adapter.notifyDataSetChanged()
                updateSelInfo()
                val totalSize = cats.flatMap { it.items }.sumOf { it.size }
                statusTv.text = "UNNEEDED FILES · ${totalSize.toReadableSize()}"
            }
        }
    }

    private fun updateSelInfo() {
        val cnt = cats.flatMap { it.items }.count { it.isSelected }
        val sz  = cats.flatMap { it.items }.filter { it.isSelected }.sumOf { it.size }
        selectedTv.text = if (cnt > 0)
            "$cnt ${getString(R.string.items_selected)} (${sz.toReadableSize()})"
        else ""
    }

    private fun doClean() {
        val selectedPkgs = cats
            .filter { it.name == "hidden_cache" }
            .flatMap { it.items.filter { i -> i.isSelected } }
            .map { it.packageName }

        if (AutoCleanService.instance != null && selectedPkgs.isNotEmpty()) {
            cleanBtn.isEnabled = false
            cleanBtn.text = "পরিষ্কার হচ্ছে..."
            AutoCleanService.onDone = {
                runOnUiThread {
                    cleanBtn.isEnabled = true
                    cleanBtn.text = getString(R.string.cleaning_done)
                    scan()
                }
            }
            AutoCleanService.startClear(selectedPkgs, false)
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                cats.filter { it.name != "hidden_cache" }.forEach { cat ->
                    cat.items.filter { it.isSelected }.forEach { item ->
                        try { java.io.File(item.packageName).deleteRecursively() } catch (_: Exception) {}
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@QuickCleanActivity,
                        getString(R.string.cleaning_done),
                        Toast.LENGTH_SHORT
                    ).show()
                    scan()
                }
            }
        }
    }
}

class CleanAdapter(
    private val cats: List<CleanCategory>,
    private val onChange: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private fun dp(v: Int) =
        (v * android.content.res.Resources.getSystem().displayMetrics.density + 0.5f).toInt()

    override fun getItemCount() = cats.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
        }
        return object : RecyclerView.ViewHolder(v) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
        val cat = cats[pos]
        val ctx = holder.itemView.context
        val ll  = holder.itemView as LinearLayout
        ll.removeAllViews()
        val cardColor = ThemeHelper.cardColor(ctx)

        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cardColor)
                cornerRadius = dp(16).toFloat()
                setStroke(1, ThemeHelper.dividerColor(ctx))
            }
        }

        val hdr = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        hdr.addView(CheckBox(ctx).apply {
            isChecked = cat.isSelected
            setOnCheckedChangeListener { _, c ->
                cat.isSelected = c
                cat.items.forEach { it.isSelected = c }
                notifyItemChanged(pos)
                onChange()
            }
        })
        val catLabel = when (cat.name) {
            "hidden_cache"  -> ctx.getString(R.string.hidden_caches)
            "empty_folders" -> ctx.getString(R.string.empty_folders)
            "apk_files"     -> "APK ফাইল"
            else            -> cat.name
        }
        hdr.addView(TextView(ctx).apply {
            text = catLabel; textSize = 15f
            setTextColor(ThemeHelper.textPrimary(ctx))
            typeface = Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, -2, 1f))
        hdr.addView(TextView(ctx).apply {
            text = cat.totalSize.toReadableSize(); textSize = 13f
            setTextColor(ThemeHelper.textSecondary(ctx))
            setPadding(0, 0, dp(8), 0)
        })
        hdr.addView(TextView(ctx).apply {
            text = if (cat.isExpanded) "▲" else "▼"
            textSize = 13f
            setTextColor(ThemeHelper.textSecondary(ctx))
        })
        hdr.setOnClickListener { cat.isExpanded = !cat.isExpanded; notifyItemChanged(pos) }
        card.addView(hdr)

        if (cat.isExpanded) {
            card.addView(android.view.View(ctx).apply {
                setBackgroundColor(ThemeHelper.dividerColor(ctx))
            }, LinearLayout.LayoutParams(-1, 1).apply { setMargins(dp(16), 0, dp(16), 0) })

            cat.items.take(15).forEach { item ->
                val row = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(16), dp(10), dp(16), dp(10))
                }
                if (item.icon != null) {
                    row.addView(ImageView(ctx).apply {
                        setImageDrawable(item.icon)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }, LinearLayout.LayoutParams(dp(38), dp(38)).apply { rightMargin = dp(12) })
                } else {
                    row.addView(ImageView(ctx).apply {
                        setImageResource(R.drawable.ic_apps)
                        imageTintList = android.content.res.ColorStateList.valueOf(
                            ThemeHelper.textSecondary(ctx))
                    }, LinearLayout.LayoutParams(dp(38), dp(38)).apply { rightMargin = dp(12) })
                }
                val nc = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
                nc.addView(TextView(ctx).apply {
                    text = item.title; textSize = 14f
                    setTextColor(ThemeHelper.textPrimary(ctx))
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                })
                nc.addView(TextView(ctx).apply {
                    text = item.size.toReadableSize(); textSize = 12f
                    setTextColor(ThemeHelper.textSecondary(ctx))
                })
                row.addView(nc, LinearLayout.LayoutParams(0, -2, 1f))
                row.addView(CheckBox(ctx).apply {
                    isChecked = item.isSelected
                    setOnCheckedChangeListener { _, c -> item.isSelected = c; onChange() }
                })
                card.addView(row)
            }
        }

        ll.addView(card, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(10)) })
    }
}
