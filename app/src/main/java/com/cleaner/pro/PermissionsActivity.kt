package com.cleaner.pro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionsActivity : BaseActivity() {

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()
    private lateinit var listContainer: LinearLayout

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        ThemeHelper.applyTheme(this)
        window.statusBarColor = ThemeHelper.bgColor(this)
        supportActionBar?.hide()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeHelper.bgColor(this@PermissionsActivity))
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(44), dp(16), dp(14))
            setBackgroundColor(ThemeHelper.navColor(this@PermissionsActivity))
        }
        top.addView(TextView(this).apply {
            text = "←"; textSize = 22f
            setTextColor(ThemeHelper.textPrimary(this@PermissionsActivity))
            setPadding(0, 0, dp(16), 0); setOnClickListener { finish() }
        })
        top.addView(TextView(this).apply {
            text = getString(R.string.permissions); textSize = 18f
            setTextColor(ThemeHelper.textPrimary(this@PermissionsActivity))
            typeface = Typeface.DEFAULT_BOLD
        })
        root.addView(top)

        val scroll = ScrollView(this)
        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(24))
        }
        scroll.addView(listContainer)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    override fun onResume() { super.onResume(); buildList() }

    private fun buildList() {
        listContainer.removeAllViews()
        data class Perm(
            val icon: Int,
            val title: String,
            val desc: String,
            val ok: Boolean,
            val action: () -> Unit
        )

        val perms = listOf(
            Perm(R.drawable.ic_apps,
                getString(R.string.perm_storage),
                getString(R.string.perm_storage_desc),
                checkStorage()) { reqStorage() },
            Perm(R.drawable.ic_clean,
                getString(R.string.perm_usage),
                getString(R.string.perm_usage_desc),
                StorageHelper.hasUsageStatsPermission(this)) {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            },
            Perm(R.drawable.ic_sleep,
                getString(R.string.perm_accessibility),
                getString(R.string.perm_accessibility_desc),
                AutoCleanService.instance != null) {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        )

        perms.forEach { perm ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(16))
                background = GradientDrawable().apply {
                    setColor(ThemeHelper.cardColor(this@PermissionsActivity))
                    cornerRadius = dp(14).toFloat()
                    setStroke(1, if (perm.ok) Color.parseColor("#1E5E1E")
                                 else ThemeHelper.dividerColor(this@PermissionsActivity))
                }
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                    setMargins(0, 0, 0, dp(10))
                }
            }
            card.addView(ImageView(this).apply {
                setImageResource(perm.icon)
                imageTintList = android.content.res.ColorStateList.valueOf(
                    if (perm.ok) Color.parseColor("#4CAF50")
                    else ThemeHelper.textSecondary(this@PermissionsActivity)
                )
            }, LinearLayout.LayoutParams(dp(26), dp(26)).apply { rightMargin = dp(14) })

            val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            col.addView(TextView(this).apply {
                text = perm.title; textSize = 14f
                setTextColor(ThemeHelper.textPrimary(this@PermissionsActivity))
                typeface = Typeface.DEFAULT_BOLD
            })
            col.addView(TextView(this).apply {
                text = perm.desc; textSize = 11f
                setTextColor(ThemeHelper.textSecondary(this@PermissionsActivity))
            })
            card.addView(col, LinearLayout.LayoutParams(0, -2, 1f))

            card.addView(TextView(this).apply {
                text = if (perm.ok) getString(R.string.granted) else getString(R.string.grant)
                textSize = 12f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (perm.ok) Color.parseColor("#4CAF50") else Color.WHITE)
                setPadding(dp(12), dp(8), dp(12), dp(8))
                if (!perm.ok) {
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#4A90E2"))
                        cornerRadius = dp(20).toFloat()
                    }
                    setOnClickListener { perm.action() }
                }
            })
            listContainer.addView(card)
        }
    }

    private fun checkStorage() =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R)
            Environment.isExternalStorageManager()
        else ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

    private fun reqStorage() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R)
            startActivity(Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:$packageName")
            ))
        else ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            100
        )
    }
}
