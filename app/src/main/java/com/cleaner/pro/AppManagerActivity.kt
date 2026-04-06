package com.cleaner.pro

import android.app.ActivityManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class AppManagerActivity : BaseActivity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    private val apps = mutableListOf<AppInfo>()
    private lateinit var adapter: AppListAdapter
    private lateinit var countTv: TextView
    private lateinit var selTv: TextView
    private lateinit var filterRow: LinearLayout
    private var currentFilter = "all"

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        ThemeHelper.applyTheme(this)
        window.statusBarColor = ThemeHelper.bgColor(this)
        supportActionBar?.hide()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeHelper.bgColor(this@AppManagerActivity))
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(44), dp(16), dp(12))
            setBackgroundColor(ThemeHelper.navColor(this@AppManagerActivity))
        }
        top.addView(TextView(this).apply {
            text = "←"; textSize = 22f
            setTextColor(ThemeHelper.textPrimary(this@AppManagerActivity))
            setPadding(0, 0, dp(16), 0)
            setOnClickListener { finish() }
        })
        top.addView(TextView(this).apply {
            text = getString(R.string.all_apps); textSize = 18f
            setTextColor(ThemeHelper.textPrimary(this@AppManagerActivity))
            typeface = Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(top)

        // ✅ Fix: filterRow কে variable এ রাখো যাতে rebuild করা যায়
        filterRow = buildFilterRow()
        root.addView(filterRow)

        val infoRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(4))
        }
        countTv = TextView(this).apply {
            textSize = 12f
            setTextColor(ThemeHelper.textSecondary(this@AppManagerActivity))
        }
        infoRow.addView(countTv, LinearLayout.LayoutParams(0, -2, 1f))
        infoRow.addView(TextView(this).apply {
            text = "SELECT ALL"; textSize = 12f
            setTextColor(Color.parseColor("#4A90E2"))
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                apps.forEach { it.isSelected = true }
                adapter.notifyDataSetChanged()
                updateSelBar()
            }
        })
        root.addView(infoRow)

        val rv = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(context)
        }
        adapter = AppListAdapter(apps, { app, action -> handleAction(app, action) }) { updateSelBar() }
        rv.adapter = adapter
        root.addView(rv, LinearLayout.LayoutParams(-1, 0, 1f))

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(20))
            setBackgroundColor(ThemeHelper.navColor(this@AppManagerActivity))
        }
        selTv = TextView(this).apply {
            textSize = 12f; setTextColor(Color.parseColor("#4A90E2"))
            gravity = Gravity.CENTER; setPadding(0, 0, 0, dp(8))
        }
        bottom.addView(selTv)

        val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(
            Triple(R.drawable.ic_sleep, "IGNORE", "#4A90E2"),
            Triple(R.drawable.ic_sleep, "SLEEP", "#F5A623"),
            Triple(R.drawable.ic_clean, "FORCE STOP", "#F5A623"),
            Triple(R.drawable.ic_apps, "UNINSTALL", "#4A90E2")
        ).forEachIndexed { i, (icon, label, col) ->
            val btn = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                setPadding(0, dp(6), 0, dp(6))
                setOnClickListener { onBottomAction(i) }
            }
            btn.addView(ImageView(this).apply {
                setImageResource(icon)
                imageTintList = ColorStateList.valueOf(Color.parseColor(col))
            }, LinearLayout.LayoutParams(dp(22), dp(22)))
            btn.addView(TextView(this).apply {
                text = label; textSize = 9f
                setTextColor(Color.parseColor(col))
                gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD
            })
            btnRow.addView(btn, LinearLayout.LayoutParams(0, -2, 1f))
        }
        bottom.addView(btnRow)
        root.addView(bottom)
        setContentView(root)
        loadApps()
    }

    // ✅ Fix: buildFilterRow() rebuild করে এবং active state সঠিকভাবে দেখায়
    private fun buildFilterRow(): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(6), dp(16), dp(6))
            setBackgroundColor(ThemeHelper.navColor(this@AppManagerActivity))
        }
        listOf(
            "all" to getString(R.string.all_apps),
            "user" to "Installed",
            "system" to "System"
        ).forEach { (key, label) ->
            val active = (key == currentFilter)
            row.addView(TextView(this).apply {
                text = label; textSize = 13f; gravity = Gravity.CENTER
                setPadding(dp(14), dp(7), dp(14), dp(7))
                setTextColor(if (active) Color.WHITE else ThemeHelper.textSecondary(this@AppManagerActivity))
                typeface = if (active) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                background = if (active) GradientDrawable().apply {
                    setColor(Color.parseColor("#4A90E2"))
                    cornerRadius = dp(20).toFloat()
                } else null
                setOnClickListener {
                    if (currentFilter != key) {
                        currentFilter = key
                        // ✅ Fix: filterRow rebuild করো tab switch করলে
                        rebuildFilterRow()
                        loadApps()
                    }
                }
            }, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(8) })
        }
        return row
    }

    private fun rebuildFilterRow() {
        val parent = filterRow.parent as? LinearLayout ?: return
        val idx = (0 until parent.childCount).firstOrNull { parent.getChildAt(it) == filterRow } ?: return
        val newRow = buildFilterRow()
        parent.removeView(filterRow)
        parent.addView(newRow, idx)
        filterRow = newRow
    }

    private fun loadApps() {
        CoroutineScope(Dispatchers.IO).launch {
            val list = StorageHelper.getInstalledApps(this@AppManagerActivity, currentFilter)
            withContext(Dispatchers.Main) {
                apps.clear(); apps.addAll(list)
                adapter.notifyDataSetChanged()
                countTv.text = "${list.size} TOTAL (${list.sumOf { it.totalSize }.toReadableSize()})"
                updateSelBar()
            }
        }
    }

    private fun updateSelBar() {
        val sel = apps.filter { it.isSelected }
        selTv.text = if (sel.isEmpty()) ""
        else "${sel.size} SELECTED (${sel.sumOf { it.totalSize }.toReadableSize()})"
    }

    private fun handleAction(app: AppInfo, action: String) {
        when (action) {
            "details" -> startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${app.packageName}")
            })
            "uninstall" -> startActivity(Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:${app.packageName}")
            })
            "sleep" -> {
                (getSystemService(ACTIVITY_SERVICE) as ActivityManager)
                    .killBackgroundProcesses(app.packageName)
                Toast.makeText(this, "${app.name} stopped", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onBottomAction(i: Int) {
        val sel = apps.filter { it.isSelected }
        if (sel.isEmpty()) {
            Toast.makeText(this, "কোনো app সিলেক্ট করা হয়নি", Toast.LENGTH_SHORT).show()
            return
        }
        when (i) {
            0 -> { // Ignore (deselect)
                sel.forEach { it.isSelected = false }
                adapter.notifyDataSetChanged()
                updateSelBar()
            }
            1 -> { // Sleep
                val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
                sel.forEach { am.killBackgroundProcesses(it.packageName) }
                Toast.makeText(this, "${sel.size} app stopped", Toast.LENGTH_SHORT).show()
            }
            2 -> { // ✅ Fix: Force Stop — শুধু selected apps এর জন্য Accessibility ব্যবহার করো
                val svc = AutoCleanService.instance
                if (svc != null) {
                    val progDialog = android.app.ProgressDialog(this).apply {
                        setMessage("পরিষ্কার হচ্ছে... (0/${sel.size})")
                        setCancelable(false)
                        show()
                    }
                    AutoCleanService.onProgress = { _, done, total ->
                        runOnUiThread {
                            progDialog.setMessage("পরিষ্কার হচ্ছে... ($done/$total)")
                        }
                    }
                    AutoCleanService.onDone = {
                        runOnUiThread {
                            progDialog.dismiss()
                            Toast.makeText(this, "${sel.size} app cache cleared", Toast.LENGTH_SHORT).show()
                            loadApps()
                        }
                    }
                    // ✅ Fix: শুধু selected apps এর package list দাও
                    AutoCleanService.startClear(sel.map { it.packageName }, false)
                } else {
                    // Accessibility নেই — fallback: kill processes
                    val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
                    sel.forEach { am.killBackgroundProcesses(it.packageName) }
                    Toast.makeText(
                        this,
                        "Accessibility enable করলে Cache clear হবে। এখন শুধু Force Stop করা হলো।",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            3 -> { // Uninstall (first selected)
                sel.firstOrNull()?.let { handleAction(it, "uninstall") }
            }
        }
    }
}

class AppListAdapter(
    private val apps: List<AppInfo>,
    private val onAction: (AppInfo, String) -> Unit,
    private val onChange: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private fun dp(v: Int) = (v * android.content.res.Resources.getSystem().displayMetrics.density + 0.5f).toInt()

    override fun getItemCount() = apps.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LinearLayout(parent.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(10))
        }
        return object : RecyclerView.ViewHolder(v) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
        val app = apps[pos]
        val ctx = holder.itemView.context
        val row = holder.itemView as LinearLayout
        row.removeAllViews()

        // ✅ Fix: পুরো row কালো না করে শুধু subtle highlight দাও
        row.background = if (app.isSelected) GradientDrawable().apply {
            setColor(Color.parseColor("#1A3060"))  // সূক্ষ্ম নীলাভ highlight
            cornerRadius = dp(8).toFloat()
        } else null

        // Checkbox — ✅ শুধু টিক মার্ক, পুরো row dark না
        val cb = CheckBox(ctx).apply {
            isChecked = app.isSelected
            buttonTintList = ColorStateList.valueOf(Color.parseColor("#4A90E2"))
            setOnCheckedChangeListener { _, c ->
                app.isSelected = c
                // ✅ Fix: শুধু এই row এর background পরিবর্তন করো
                row.background = if (c) GradientDrawable().apply {
                    setColor(Color.parseColor("#1A3060"))
                    cornerRadius = dp(8).toFloat()
                } else null
                onChange()
            }
        }
        row.addView(cb, LinearLayout.LayoutParams(dp(32), dp(32)).apply { rightMargin = dp(4) })

        // App icon
        row.addView(ImageView(ctx).apply {
            app.icon?.let { setImageDrawable(it) }
            scaleType = ImageView.ScaleType.FIT_CENTER
        }, LinearLayout.LayoutParams(dp(42), dp(42)).apply { rightMargin = dp(12) })

        // App info
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        col.addView(TextView(ctx).apply {
            text = app.name; textSize = 14f
            setTextColor(ThemeHelper.textPrimary(ctx))
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        })
        val days = if (app.lastUsed > 0) {
            val d = ((System.currentTimeMillis() - app.lastUsed) / (1000L * 60 * 60 * 24)).toInt()
            when { d == 0 -> "আজ"; d == 1 -> "গতকাল"; else -> "$d দিন আগে" }
        } else ""
        col.addView(TextView(ctx).apply {
            text = if (days.isNotEmpty()) "${app.totalSize.toReadableSize()} · $days"
            else app.totalSize.toReadableSize()
            textSize = 12f
            setTextColor(ThemeHelper.textSecondary(ctx))
        })
        row.addView(col, LinearLayout.LayoutParams(0, -2, 1f))

        if (app.isSystemApp) {
            row.addView(TextView(ctx).apply {
                text = "SYS"; textSize = 9f
                setTextColor(ThemeHelper.textSecondary(ctx))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1E2E4A"))
                    cornerRadius = dp(4).toFloat()
                }
                setPadding(dp(4), dp(2), dp(4), dp(2))
            })
        }

        row.setOnClickListener { onAction(app, "details") }
        row.setOnLongClickListener {
            val m = PopupMenu(ctx, row)
            m.menu.add(ctx.getString(R.string.clear_cache))
            m.menu.add("Force Stop")
            m.menu.add(ctx.getString(R.string.uninstall))
            m.setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    ctx.getString(R.string.clear_cache), "Force Stop" -> onAction(app, "sleep")
                    else -> onAction(app, "uninstall")
                }; true
            }
            m.show(); true
        }
    }
}
