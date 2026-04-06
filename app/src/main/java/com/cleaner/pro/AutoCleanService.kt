package com.cleaner.pro

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AutoCleanService : AccessibilityService() {

    companion object {
        var instance: AutoCleanService? = null

        // ✅ Fix: isRunning শুধু startClear() call করলে true হবে
        var isRunning = false
            private set

        val clearQueue = ArrayDeque<Pair<String, Boolean>>()
        var onProgress: ((String, Int, Int) -> Unit)? = null
        var onDone: (() -> Unit)? = null
        var totalPackages = 0
        var donePackages = 0

        // ✅ Fix: বর্তমানে কোন package process হচ্ছে track করা
        private var currentTargetPackage = ""

        fun startClear(packages: List<String>, clearStorage: Boolean) {
            clearQueue.clear()
            packages.forEach { clearQueue.add(Pair(it, clearStorage)) }
            totalPackages = packages.size
            donePackages = 0
            instance?.processNext()
        }

        fun stopAll() {
            isRunning = false
            clearQueue.clear()
            currentTargetPackage = ""
            instance?.handler?.removeCallbacksAndMessages(null)
        }
    }

    internal val handler = Handler(Looper.getMainLooper())
    private var stepState = 0
    private var doClearStorage = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isRunning = false
    }

    override fun onInterrupt() {
        isRunning = false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // ✅ Fix 1: isRunning false হলে কিছুই করবে না
        if (!isRunning) return

        // ✅ Fix 2: currentTargetPackage empty হলে কিছুই করবে না
        if (currentTargetPackage.isEmpty()) return

        val t = event?.eventType ?: return
        if (t != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            t != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return

        // ✅ Fix 3: Event শুধু Settings app থেকে আসলে process করবে
        val eventPkg = event.packageName?.toString() ?: ""
        val isSettingsApp = eventPkg == "com.android.settings" ||
                eventPkg.contains("settings", ignoreCase = true) ||
                eventPkg == "com.google.android.packageinstaller"
        if (!isSettingsApp) return

        val root = rootInActiveWindow ?: return

        try {
            when (stepState) {
                1 -> {
                    if (clickText(root, listOf(
                            "Storage & cache", "Storage", "Storage and cache",
                            "স্টোরেজ", "স্টোরেজ এবং ক্যাশ"
                        ))) {
                        stepState = 2
                    }
                }
                2 -> {
                    if (doClearStorage) {
                        // Clear Storage বাটনে ক্লিক
                        if (clickText(root, listOf(
                                "Clear storage", "CLEAR STORAGE",
                                "Clear data", "CLEAR DATA"
                            ))) {
                            handler.postDelayed({
                                // Confirm dialog
                                val r2 = rootInActiveWindow
                                if (r2 != null) {
                                    clickText(r2, listOf("OK", "Delete", "Clear", "হ্যাঁ"))
                                    try { r2.recycle() } catch (_: Exception) {}
                                }
                                handler.postDelayed({ doNext() }, 800)
                            }, 600)
                        }
                    } else {
                        // Clear Cache বাটনে ক্লিক
                        if (clickText(root, listOf("Clear cache", "CLEAR CACHE", "ক্যাশ পরিষ্কার"))) {
                            handler.postDelayed({ doNext() }, 800)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        try { root.recycle() } catch (_: Exception) {}
    }

    fun processNext() {
        if (clearQueue.isEmpty()) {
            isRunning = false
            currentTargetPackage = ""
            handler.post {
                onDone?.invoke()
                try { performGlobalAction(GLOBAL_ACTION_HOME) } catch (_: Exception) {}
            }
            return
        }

        val (pkg, cs) = clearQueue.first()
        currentTargetPackage = pkg
        doClearStorage = cs
        stepState = 1
        isRunning = true
        donePackages = totalPackages - clearQueue.size
        onProgress?.invoke(pkg, donePackages, totalPackages)

        handler.postDelayed({
            try {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$pkg")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } catch (_: Exception) {
                doNext()
            }
        }, 600)
    }

    private fun doNext() {
        if (clearQueue.isNotEmpty()) clearQueue.removeFirst()
        donePackages++
        currentTargetPackage = ""

        handler.postDelayed({
            try { performGlobalAction(GLOBAL_ACTION_BACK) } catch (_: Exception) {}
            handler.postDelayed({
                try { performGlobalAction(GLOBAL_ACTION_BACK) } catch (_: Exception) {}
                handler.postDelayed({ processNext() }, 700)
            }, 400)
        }, 300)
    }

    private fun clickText(root: AccessibilityNodeInfo, texts: List<String>): Boolean {
        for (text in texts) {
            try {
                root.findAccessibilityNodeInfosByText(text).forEach { node ->
                    var target: AccessibilityNodeInfo? = node
                    var depth = 0
                    while (target != null && !target.isClickable && depth < 5) {
                        target = target.parent
                        depth++
                    }
                    if (target?.isEnabled == true &&
                        target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        try { node.recycle() } catch (_: Exception) {}
                        return true
                    }
                    try { node.recycle() } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
        return false
    }
}
