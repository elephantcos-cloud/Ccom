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
        var isRunning = false
        val clearQueue = ArrayDeque<Pair<String, Boolean>>()
        var onProgress: ((String, Int, Int) -> Unit)? = null
        var onDone: (() -> Unit)? = null
        var totalPackages = 0
        var donePackages = 0

        fun startClear(packages: List<String>, clearStorage: Boolean) {
            clearQueue.clear()
            packages.forEach { clearQueue.add(Pair(it, clearStorage)) }
            totalPackages = packages.size
            donePackages = 0
            instance?.processNext()
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var stepState = 0

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
        if (!isRunning) return
        val t = event?.eventType ?: return
        if (t != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            t != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return
        val root = rootInActiveWindow ?: return

        when (stepState) {
            1 -> if (clickText(root, listOf("Storage & cache", "Storage", "Storage and cache"))) {
                stepState = 2
            }
            2 -> {
                if (clickText(root, listOf("Clear cache", "CLEAR CACHE"))) {
                    handler.postDelayed({ doNext() }, 800)
                }
            }
        }
        root.recycle()
    }

    fun processNext() {
        if (clearQueue.isEmpty()) {
            isRunning = false
            handler.post {
                onDone?.invoke()
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
            return
        }
        val (pkg, _) = clearQueue.first()
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
            } catch (_: Exception) { doNext() }
        }, 500)
    }

    private fun doNext() {
        if (clearQueue.isNotEmpty()) clearQueue.removeFirst()
        donePackages++
        handler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_BACK)
            handler.postDelayed({
                performGlobalAction(GLOBAL_ACTION_BACK)
                handler.postDelayed({ processNext() }, 600)
            }, 400)
        }, 300)
    }

    private fun clickText(root: AccessibilityNodeInfo, texts: List<String>): Boolean {
        for (text in texts) {
            root.findAccessibilityNodeInfosByText(text).forEach { node ->
                var target: AccessibilityNodeInfo? = node
                while (target != null && !target.isClickable) target = target.parent
                if (target?.isEnabled == true &&
                    target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    node.recycle()
                    return true
                }
                node.recycle()
            }
        }
        return false
    }
}
