package com.nightmareblocker.reels

import android.accessibilityservice.AccessibilityService
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageView
import android.widget.TextView
import kotlin.random.Random

/**
 * Watches Instagram's on-screen UI for signs the user has opened Reels,
 * and throws a full-screen horror overlay over it when detected.
 *
 * DETECTION NOTE: Instagram does not expose a stable "this is Reels" flag.
 * This matches against resource-id fragments and content-descriptions that
 * have been observed in Instagram's Reels UI. Instagram changes its internal
 * view structure over time, so if detection stops working, see README.md for
 * how to find current IDs with `adb shell uiautomator dump` and update
 * REEL_RESOURCE_ID_FRAGMENTS / REEL_CONTENT_DESCRIPTIONS below.
 */
class ReelsAccessibilityService : AccessibilityService() {

    companion object {
        private const val INSTAGRAM_PACKAGE = "com.instagram.android"

        // Edit this list if Instagram updates its internal view IDs and detection breaks.
        val REEL_RESOURCE_ID_FRAGMENTS = listOf(
            "clips_tab",
            "clips_viewer_view_pager",
            "clips_swipe_refresh_container",
            "reel_viewer_fragment",
            "clips_viewer_fragment",
            "clips_"
        )
        val REEL_CONTENT_DESCRIPTIONS = listOf("reels", "reel")
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var heartbeatPlayer: MediaPlayer? = null
    private var screamPlayer: MediaPlayer? = null
    private var laughPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastCheckMs = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!Prefs.isEnabled(this)) {
            removeOverlay()
            return
        }

        val pkg = event.packageName?.toString()
        if (pkg != INSTAGRAM_PACKAGE) {
            // Left Instagram entirely — clear any overlay/sound.
            removeOverlay()
            return
        }

        // Throttle tree scans; content-changed events fire very frequently.
        val now = System.currentTimeMillis()
        if (now - lastCheckMs < 250) return
        lastCheckMs = now

        val root = rootInActiveWindow ?: return
        val onReels = try {
            containsReelsIndicator(root)
        } finally {
            root.recycle()
        }

        if (onReels) {
            showOverlay()
        } else {
            removeOverlay()
        }
    }

    private fun containsReelsIndicator(node: AccessibilityNodeInfo, depth: Int = 0): Boolean {
        if (depth > 40) return false // safety guard against pathological trees

        val resId = node.viewIdResourceName
        if (resId != null) {
            for (fragment in REEL_RESOURCE_ID_FRAGMENTS) {
                if (resId.contains(fragment, ignoreCase = true)) return true
            }
        }

        val desc = node.contentDescription?.toString()
        if (desc != null) {
            for (candidate in REEL_CONTENT_DESCRIPTIONS) {
                if (desc.equals(candidate, ignoreCase = true)) return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = try {
                containsReelsIndicator(child, depth + 1)
            } finally {
                child.recycle()
            }
            if (found) return true
        }
        return false
    }

    // ---------- Overlay ----------

    private val messages = listOf(
        "THEY SEE YOU SCROLLING",
        "GET OUT",
        "NO MORE REELS FOR YOU",
        "IT KNOWS YOU'RE HERE",
        "TURN BACK NOW",
        "YOUR TIME IS UP"
    )

    private val scareImages = listOf(R.drawable.scare1, R.drawable.scare2, R.drawable.scare3)

    private fun showOverlay() {
        if (overlayView != null) return // already showing

        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_horror, null)

        val imageView = view.findViewById<ImageView>(R.id.overlayImage)
        imageView.setImageResource(scareImages[Random.nextInt(scareImages.size)])

        val messageView = view.findViewById<TextView>(R.id.overlayMessage)
        messageView.text = messages[Random.nextInt(messages.size)]
        startFlicker(messageView)

        val escapeButton = view.findViewById<View>(R.id.overlayEscapeButton)
        escapeButton.setOnClickListener {
            removeOverlay()
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER

        try {
            windowManager?.addView(view, params)
            overlayView = view
        } catch (e: Exception) {
            return
        }

        Prefs.incrementScareCount(this)
        playSounds()
    }

    private fun startFlicker(view: TextView) {
        val animator = ValueAnimator.ofFloat(1f, 0.25f, 1f)
        animator.duration = 2200
        animator.repeatCount = ValueAnimator.INFINITE
        animator.addUpdateListener { view.alpha = it.animatedValue as Float }
        animator.start()
        view.tag = animator
    }

    private fun removeOverlay() {
        val view = overlayView ?: run {
            stopSounds()
            return
        }
        (view.findViewById<TextView>(R.id.overlayMessage)?.tag as? ValueAnimator)?.cancel()
        try {
            windowManager?.removeView(view)
        } catch (e: Exception) {
            // view already detached — ignore
        }
        overlayView = null
        stopSounds()
    }

    // ---------- Sound ----------

    private fun playSounds() {
        heartbeatPlayer = MediaPlayer.create(this, R.raw.heartbeat)?.apply {
            isLooping = true
            start()
        }
        screamPlayer = MediaPlayer.create(this, R.raw.scream)?.apply { start() }
        handler.postDelayed({
            laughPlayer = MediaPlayer.create(this, R.raw.laugh)?.apply { start() }
        }, 600)
    }

    private fun stopSounds() {
        handler.removeCallbacksAndMessages(null)
        listOf(heartbeatPlayer, screamPlayer, laughPlayer).forEach {
            try {
                it?.stop()
                it?.release()
            } catch (e: Exception) { /* ignore */ }
        }
        heartbeatPlayer = null
        screamPlayer = null
        laughPlayer = null
    }

    override fun onInterrupt() {
        removeOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
    }
}
