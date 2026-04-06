package com.cleaner.pro

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*

class SplashActivity : BaseActivity() {

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        ThemeHelper.applyTheme(this)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        window.statusBarColor = Color.parseColor("#0A1628")
        window.navigationBarColor = Color.parseColor("#0A1628")
        supportActionBar?.hide()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0A1628"))
        }

        val logo = ImageView(this).apply {
            try {
                setImageResource(R.drawable.ic_logo)
            } catch (_: Exception) {
                setBackgroundColor(Color.parseColor("#1A2A44"))
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        root.addView(logo, LinearLayout.LayoutParams(dp(160), dp(160)).apply {
            gravity = Gravity.CENTER
            bottomMargin = dp(28)
        })

        root.addView(TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 34f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        })

        root.addView(TextView(this).apply {
            text = "Smart Phone Cleaner"
            textSize = 14f
            setTextColor(Color.parseColor("#8A9BBE"))
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        })

        setContentView(root)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 2000)
    }
}
