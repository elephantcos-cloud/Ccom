package com.cleaner.pro

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.drawerlayout.widget.DrawerLayout

class MainActivity : BaseActivity() {

    lateinit var drawer: DrawerLayout
    private lateinit var container: FrameLayout
    private var currentTab = 0

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        ThemeHelper.applyTheme(this)
        window.statusBarColor = ThemeHelper.bgColor(this)
        window.navigationBarColor = ThemeHelper.navColor(this)
        supportActionBar?.hide()

        drawer = DrawerLayout(this)
        drawer.addView(buildMain())
        drawer.addView(
            buildDrawer(),
            DrawerLayout.LayoutParams(
                (resources.displayMetrics.widthPixels * 0.80).toInt(),
                DrawerLayout.LayoutParams.MATCH_PARENT
            ).apply { gravity = Gravity.START }
        )

        setContentView(drawer)
        showTab(0)
    }

    private fun buildMain(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeHelper.bgColor(this@MainActivity))
            layoutParams = DrawerLayout.LayoutParams(-1, -1)
        }
        container = FrameLayout(this)
        root.addView(container, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(buildBottomNav(), LinearLayout.LayoutParams(-1, dp(60)))
        return root
    }

    private fun buildBottomNav(): LinearLayout {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(ThemeHelper.navColor(this@MainActivity))
            elevation = dp(8).toFloat()
        }
        data class Tab(val icon: Int, val label: String)
        val tabs = listOf(
            Tab(R.drawable.ic_home,     getString(R.string.nav_home)),
            Tab(R.drawable.ic_clean,    getString(R.string.nav_clean)),
            Tab(R.drawable.ic_apps,     getString(R.string.nav_apps)),
            Tab(R.drawable.ic_media,    getString(R.string.nav_media)),
            Tab(R.drawable.ic_settings, getString(R.string.nav_settings))
        )
        tabs.forEachIndexed { i, tab ->
            val active = (i == currentTab)
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(0, dp(7), 0, dp(7))
                setOnClickListener { switchTab(i) }
            }
            val tint = if (active) Color.parseColor("#4A90E2") else ThemeHelper.textSecondary(this)
            val img = ImageView(this).apply {
                setImageResource(tab.icon)
                imageTintList = ColorStateList.valueOf(tint)
            }
            col.addView(img, LinearLayout.LayoutParams(dp(22), dp(22)))
            col.addView(TextView(this).apply {
                text = tab.label
                textSize = 9f
                setTextColor(tint)
                gravity = Gravity.CENTER
                setPadding(0, dp(2), 0, 0)
                if (active) typeface = Typeface.DEFAULT_BOLD
            })
            nav.addView(col, LinearLayout.LayoutParams(0, -1, 1f))
        }
        return nav
    }

    private fun buildDrawer(): ScrollView {
        val bg = if (ThemeHelper.isDark(this)) Color.parseColor("#0A1628")
                 else Color.parseColor("#1A2A44")
        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(52), 0, dp(24))
        }

        val hdr = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(20))
        }
        try {
            hdr.addView(ImageView(this).apply {
                setImageResource(R.drawable.ic_logo)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }, LinearLayout.LayoutParams(dp(60), dp(60)).apply { bottomMargin = dp(10) })
        } catch (_: Exception) {}
        hdr.addView(TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        })
        hdr.addView(TextView(this).apply {
            text = "v1.0 · Smart Cleaner"
            textSize = 12f
            setTextColor(Color.parseColor("#8A9BBE"))
        })
        ll.addView(hdr)
        ll.addView(divider())

        ll.addView(sectionLabel("STORAGE"))
        ll.addView(drawerRow(R.drawable.ic_apps,  getString(R.string.apps))   { nav(AppManagerActivity::class.java) })
        ll.addView(drawerRow(R.drawable.ic_media, getString(R.string.photos)) { nav(MediaActivity::class.java) })
        ll.addView(drawerRow(R.drawable.ic_media, getString(R.string.videos)) { nav(MediaActivity::class.java) })
        ll.addView(drawerRow(R.drawable.ic_media, getString(R.string.audio))  { nav(MediaActivity::class.java) })
        ll.addView(divider())

        ll.addView(sectionLabel("FEATURES"))
        ll.addView(drawerRow(R.drawable.ic_clean,    getString(R.string.quick_clean)) { nav(QuickCleanActivity::class.java) })
        ll.addView(drawerRow(R.drawable.ic_sleep,    getString(R.string.sleep_mode))  { nav(AppManagerActivity::class.java) })
        ll.addView(drawerRow(R.drawable.ic_settings, getString(R.string.settings))    { nav(SettingsActivity::class.java) })
        ll.addView(divider())

        ll.addView(sectionLabel("OTHER"))
        ll.addView(drawerRow(R.drawable.ic_tips,     getString(R.string.permissions)) { nav(PermissionsActivity::class.java) })
        ll.addView(drawerRow(R.drawable.ic_settings, getString(R.string.about)) {
            drawer.closeDrawers()
            Toast.makeText(this, "${getString(R.string.app_name)} v1.0", Toast.LENGTH_SHORT).show()
        })

        scroll.addView(ll)
        return scroll
    }

    private fun <T> nav(cls: Class<T>) {
        drawer.closeDrawers()
        startActivity(Intent(this, cls))
    }

    private fun sectionLabel(text: String) = TextView(this).apply {
        this.text = text
        textSize = 11f
        setTextColor(Color.parseColor("#4A90E2"))
        typeface = Typeface.DEFAULT_BOLD
        setPadding(dp(20), dp(10), dp(20), dp(4))
    }

    private fun divider() = View(this).apply {
        setBackgroundColor(Color.parseColor("#1E2E4A"))
        layoutParams = LinearLayout.LayoutParams(-1, 1).apply {
            setMargins(dp(20), dp(4), dp(20), dp(4))
        }
    }

    private fun drawerRow(icon: Int, label: String, onClick: () -> Unit): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(14), dp(20), dp(14))
            setOnClickListener { onClick() }
        }
        row.addView(ImageView(this).apply {
            setImageResource(icon)
            imageTintList = ColorStateList.valueOf(Color.parseColor("#8A9BBE"))
        }, LinearLayout.LayoutParams(dp(22), dp(22)).apply { rightMargin = dp(16) })
        row.addView(TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(Color.WHITE)
        })
        return row
    }

    fun openDrawer() = drawer.openDrawer(Gravity.START)

    fun switchTab(i: Int) {
        currentTab = i
        when (i) {
            0 -> showTab(0)
            1 -> startActivity(Intent(this, QuickCleanActivity::class.java))
            2 -> startActivity(Intent(this, AppManagerActivity::class.java))
            3 -> startActivity(Intent(this, MediaActivity::class.java))
            4 -> startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun showTab(i: Int) {
        container.removeAllViews()
        container.addView(DashboardView(this))
    }

    override fun onResume() {
        super.onResume()
        if (currentTab == 0) showTab(0)
    }
}
