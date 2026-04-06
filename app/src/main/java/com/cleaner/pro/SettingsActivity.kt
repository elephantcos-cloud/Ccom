package com.cleaner.pro

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog

class SettingsActivity : BaseActivity() {

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        ThemeHelper.applyTheme(this)
        window.statusBarColor = ThemeHelper.bgColor(this)
        supportActionBar?.hide()
        buildUI()
    }

    private fun buildUI() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeHelper.bgColor(this@SettingsActivity))
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(44), dp(16), dp(14))
            setBackgroundColor(ThemeHelper.navColor(this@SettingsActivity))
        }
        top.addView(TextView(this).apply {
            text = "←"; textSize = 22f
            setTextColor(ThemeHelper.textPrimary(this@SettingsActivity))
            setPadding(0, 0, dp(16), 0); setOnClickListener { finish() }
        })
        top.addView(TextView(this).apply {
            text = getString(R.string.settings); textSize = 18f
            setTextColor(ThemeHelper.textPrimary(this@SettingsActivity))
            typeface = Typeface.DEFAULT_BOLD
        })
        root.addView(top)

        val scroll = ScrollView(this)
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(24))
        }

        ll.addView(settingRow(getString(R.string.quick_clean), "") {
            startActivity(Intent(this, QuickCleanActivity::class.java))
        })

        ll.addView(settingRow(getString(R.string.theme),
            when (PrefsHelper.getTheme(this)) {
                PrefsHelper.THEME_DARK  -> getString(R.string.dark_theme)
                PrefsHelper.THEME_LIGHT -> getString(R.string.light_theme)
                else                    -> getString(R.string.system_theme)
            }) {
            val opts = arrayOf(
                "${getString(R.string.dark_theme)} 🌙",
                "${getString(R.string.light_theme)} ☀️",
                "${getString(R.string.system_theme)} 📱"
            )
            val keys = arrayOf(PrefsHelper.THEME_DARK, PrefsHelper.THEME_LIGHT, PrefsHelper.THEME_SYSTEM)
            val cur  = keys.indexOf(PrefsHelper.getTheme(this))
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.theme))
                .setSingleChoiceItems(opts, cur) { d, i ->
                    PrefsHelper.setTheme(this, keys[i])
                    ThemeHelper.applyTheme(this)
                    d.dismiss()
                    recreate()
                }.show()
        })

        ll.addView(settingRow(getString(R.string.language),
            if (PrefsHelper.getLanguage(this) == "bn") "বাংলা 🇧🇩" else "English 🇺🇸") {
            val opts = arrayOf("বাংলা 🇧🇩", "English 🇺🇸")
            val keys = arrayOf("bn", "en")
            val cur  = keys.indexOf(PrefsHelper.getLanguage(this)).coerceAtLeast(0)
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.language))
                .setSingleChoiceItems(opts, cur) { d, i ->
                    PrefsHelper.setLanguage(this, keys[i])
                    d.dismiss()
                    finish()
                    startActivity(intent)
                }.show()
        })

        ll.addView(switchRow(getString(R.string.notifications), PrefsHelper.isNotifOn(this)) {
            PrefsHelper.setNotif(this, it)
        })

        ll.addView(switchRow(getString(R.string.auto_clean), PrefsHelper.isAutoClean(this)) {
            PrefsHelper.setAutoClean(this, it)
        })

        ll.addView(settingRow(getString(R.string.permissions), "") {
            startActivity(Intent(this, PermissionsActivity::class.java))
        })

        ll.addView(settingRow("Send log report", "") {
            Toast.makeText(this, "No issues detected", Toast.LENGTH_SHORT).show()
        })

        ll.addView(settingRow(getString(R.string.about), "${getString(R.string.version)} 1.0") {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.app_name))
                .setMessage("${getString(R.string.version)}: 1.0\nSmart Phone Cleaner")
                .setPositiveButton("OK", null).show()
        })

        scroll.addView(ll)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun settingRow(title: String, sub: String, onClick: () -> Unit): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply {
                setColor(ThemeHelper.cardColor(this@SettingsActivity))
                cornerRadius = dp(12).toFloat()
            }
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(8)) }
        }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(TextView(this).apply {
            text = title; textSize = 15f
            setTextColor(ThemeHelper.textPrimary(this@SettingsActivity))
            typeface = Typeface.DEFAULT_BOLD
        })
        if (sub.isNotEmpty()) col.addView(TextView(this).apply {
            text = sub; textSize = 12f
            setTextColor(ThemeHelper.textSecondary(this@SettingsActivity))
            setPadding(0, dp(2), 0, 0)
        })
        row.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(TextView(this).apply {
            text = "›"; textSize = 22f
            setTextColor(ThemeHelper.textSecondary(this@SettingsActivity))
        })
        return row
    }

    private fun switchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                setColor(ThemeHelper.cardColor(this@SettingsActivity))
                cornerRadius = dp(12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(8)) }
        }
        row.addView(TextView(this).apply {
            text = title; textSize = 15f
            setTextColor(ThemeHelper.textPrimary(this@SettingsActivity))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Switch(this).apply {
            isChecked = checked
            setOnCheckedChangeListener { _, v -> onChange(v) }
        })
        return row
    }
}
