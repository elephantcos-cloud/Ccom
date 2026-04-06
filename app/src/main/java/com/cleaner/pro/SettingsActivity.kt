package com.cleaner.pro

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class SettingsActivity : BaseActivity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        ThemeHelper.applyTheme(this)
        window.statusBarColor = ThemeHelper.bgColor(this)
        supportActionBar?.hide()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeHelper.bgColor(this@SettingsActivity))
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(44), dp(16), dp(14))
            setBackgroundColor(ThemeHelper.navColor(this@SettingsActivity))
        }
        top.addView(TextView(this).apply {
            text = "←"; textSize = 22f
            setTextColor(ThemeHelper.textPrimary(this@SettingsActivity))
            setPadding(0, 0, dp(16), 0)
            setOnClickListener { finish() }
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

        // Quick Clean
        ll.addView(settingItem(getString(R.string.quick_clean), null, true) {
            startActivity(Intent(this, QuickCleanActivity::class.java))
        })

        // Theme
        val currentTheme = PrefsHelper.getTheme(this)
        val themeLabel = when (currentTheme) {
            PrefsHelper.THEME_DARK -> getString(R.string.dark_theme)
            PrefsHelper.THEME_LIGHT -> getString(R.string.light_theme)
            else -> getString(R.string.system_theme)
        }
        ll.addView(settingItem(getString(R.string.theme), themeLabel, true) {
            showThemeDialog()
        })

        // ✅ Fix: Language selection with full app restart
        val currentLang = PrefsHelper.getLanguage(this)
        val langLabel = if (currentLang == "en") "English 🇺🇸" else "বাংলা 🇧🇩"
        ll.addView(settingItem(getString(R.string.language), langLabel, true) {
            showLanguageDialog()
        })

        // Notification toggle
        val notifSwitch = Switch(this).apply {
            isChecked = PrefsHelper.isNotifOn(this@SettingsActivity)
            thumbTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#4A90E2"))
            setOnCheckedChangeListener { _, c -> PrefsHelper.setNotif(this@SettingsActivity, c) }
        }
        ll.addView(settingItemWithWidget(getString(R.string.notifications), notifSwitch))

        // Auto clean toggle
        val autoSwitch = Switch(this).apply {
            isChecked = PrefsHelper.isAutoClean(this@SettingsActivity)
            thumbTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#4A90E2"))
            setOnCheckedChangeListener { _, c -> PrefsHelper.setAutoClean(this@SettingsActivity, c) }
        }
        ll.addView(settingItemWithWidget(getString(R.string.auto_clean), autoSwitch))

        // Permissions
        ll.addView(settingItem(getString(R.string.permissions), null, true) {
            startActivity(Intent(this, PermissionsActivity::class.java))
        })

        // Send log report
        ll.addView(settingItem("Send log report", null, true) {
            Toast.makeText(this, "কোনো log নেই", Toast.LENGTH_SHORT).show()
        })

        // About — এখন AboutActivity তে যাবে
        ll.addView(settingItem(getString(R.string.about), "${getString(R.string.version)} 1.0", true) {
            startActivity(Intent(this, AboutActivity::class.java))
        })

        scroll.addView(ll)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun settingItem(
        title: String, subtitle: String?, arrow: Boolean,
        onClick: (() -> Unit)? = null
    ): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply {
                setColor(ThemeHelper.cardColor(this@SettingsActivity))
                cornerRadius = dp(14).toFloat()
                setStroke(1, ThemeHelper.dividerColor(this@SettingsActivity))
            }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                setMargins(0, 0, 0, dp(10))
            }
            onClick?.let { setOnClickListener { it() } }
        }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(TextView(this).apply {
            text = title; textSize = 15f
            setTextColor(ThemeHelper.textPrimary(this@SettingsActivity))
            typeface = Typeface.DEFAULT_BOLD
        })
        if (subtitle != null) {
            col.addView(TextView(this).apply {
                text = subtitle; textSize = 12f
                setTextColor(ThemeHelper.textSecondary(this@SettingsActivity))
            })
        }
        card.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
        if (arrow) {
            card.addView(TextView(this).apply {
                text = "›"; textSize = 22f
                setTextColor(ThemeHelper.textSecondary(this@SettingsActivity))
            })
        }
        return card
    }

    private fun settingItemWithWidget(title: String, widget: android.view.View): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = GradientDrawable().apply {
                setColor(ThemeHelper.cardColor(this@SettingsActivity))
                cornerRadius = dp(14).toFloat()
                setStroke(1, ThemeHelper.dividerColor(this@SettingsActivity))
            }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }
        card.addView(TextView(this).apply {
            text = title; textSize = 15f
            setTextColor(ThemeHelper.textPrimary(this@SettingsActivity))
            typeface = Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(widget)
        return card
    }

    private fun showThemeDialog() {
        val options = arrayOf(
            getString(R.string.light_theme),
            getString(R.string.dark_theme),
            getString(R.string.system_theme)
        )
        val keys = arrayOf(
            PrefsHelper.THEME_LIGHT,
            PrefsHelper.THEME_DARK,
            PrefsHelper.THEME_SYSTEM
        )
        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.theme))
            .setItems(options) { _, i ->
                PrefsHelper.setTheme(this, keys[i])
                ThemeHelper.applyTheme(this)
                recreate()
            }.show()
    }

    private fun showLanguageDialog() {
        val options = arrayOf("বাংলা 🇧🇩", "English 🇺🇸")
        val keys = arrayOf("bn", "en")
        val current = PrefsHelper.getLanguage(this)
        val currentIdx = if (current == "en") 1 else 0

        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.language))
            .setSingleChoiceItems(options, currentIdx) { dialog, i ->
                dialog.dismiss()
                PrefsHelper.setLanguage(this, keys[i])
                // ✅ Fix: পুরো app restart করতে হবে locale apply করতে
                val intent = Intent(this, SplashActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
                finishAffinity()
            }.show()
    }
}
