package com.cleaner.pro

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class AboutActivity : BaseActivity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        ThemeHelper.applyTheme(this)
        window.statusBarColor = ThemeHelper.bgColor(this)
        supportActionBar?.hide()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeHelper.bgColor(this@AboutActivity))
        }

        // Top bar
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(44), dp(16), dp(14))
            setBackgroundColor(ThemeHelper.navColor(this@AboutActivity))
        }
        top.addView(TextView(this).apply {
            text = "←"; textSize = 22f
            setTextColor(ThemeHelper.textPrimary(this@AboutActivity))
            setPadding(0, 0, dp(16), 0)
            setOnClickListener { finish() }
        })
        top.addView(TextView(this).apply {
            text = getString(R.string.about); textSize = 18f
            setTextColor(ThemeHelper.textPrimary(this@AboutActivity))
            typeface = Typeface.DEFAULT_BOLD
        })
        root.addView(top)

        val scroll = ScrollView(this)
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(40), dp(24), dp(32))
        }

        // Logo
        try {
            ll.addView(
                ImageView(this).apply {
                    setImageResource(R.drawable.ic_logo)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                },
                LinearLayout.LayoutParams(dp(100), dp(100)).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    bottomMargin = dp(20)
                }
            )
        } catch (_: Exception) {}

        // App name
        ll.addView(TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 26f
            setTextColor(ThemeHelper.textPrimary(this@AboutActivity))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })

        // Version
        ll.addView(TextView(this).apply {
            text = "${getString(R.string.version)} 1.0 · Smart Cleaner"
            textSize = 13f
            setTextColor(ThemeHelper.textSecondary(this@AboutActivity))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(40) })

        // Divider
        ll.addView(android.view.View(this).apply {
            setBackgroundColor(ThemeHelper.dividerColor(this@AboutActivity))
        }, LinearLayout.LayoutParams(-1, 1).apply { bottomMargin = dp(28) })

        // Facebook button
        ll.addView(
            socialBtn(
                icon = "📘",
                text = "  Facebook এ Follow করুন",
                color = Color.parseColor("#1877F2")
            ) {
                openUrl("https://www.facebook.com/msohan.open")
            }
        )

        ll.addView(
            android.view.View(this),
            LinearLayout.LayoutParams(-1, dp(12))
        )

        // Telegram button
        ll.addView(
            socialBtn(
                icon = "✈️",
                text = "  Telegram Group এ যোগ দিন",
                color = Color.parseColor("#2AABEE")
            ) {
                openUrl("https://t.me/mid_nightroom")
            }
        )

        ll.addView(
            android.view.View(this),
            LinearLayout.LayoutParams(-1, dp(40))
        )

        // Footer
        ll.addView(TextView(this).apply {
            text = "Made with ❤️ by Shohan"
            textSize = 12f
            setTextColor(ThemeHelper.textSecondary(this@AboutActivity))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, -2))

        scroll.addView(ll)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun socialBtn(icon: String, text: String, color: Int, onClick: () -> Unit): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(16), dp(20), dp(16))
            background = GradientDrawable().apply {
                setColor(color)
                cornerRadius = dp(14).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(-1, -2)
            setOnClickListener { onClick() }
            elevation = dp(2).toFloat()
        }
        row.addView(TextView(this).apply {
            this.text = icon
            textSize = 20f
        })
        row.addView(TextView(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        })
        return row
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(this, "Browser খুলতে পারছে না", Toast.LENGTH_SHORT).show()
        }
    }
}
