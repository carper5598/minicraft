package com.example.minicraft

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout

class MainActivity : Activity() {

    private lateinit var gameView: GameView
    private val hotbarButtons = arrayOfNulls<Button>(4)
    private val handler = Handler(Looper.getMainLooper())

    private val blockNames = arrayOf("", "Трава", "Земля", "Камень")

    private val hotbarUpdater = object : Runnable {
        override fun run() {
            refreshHotbar()
            handler.postDelayed(this, 150)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gameView = GameView(this)
        val root = FrameLayout(this)
        root.addView(
            gameView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val cross = CrosshairView(this)
        root.addView(
            cross,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        buildHotbar(root)

        val input = gameView.input
        val start = Gravity.BOTTOM or Gravity.START
        val end = Gravity.BOTTOM or Gravity.END

        holdButton(root, "↑", start, 80, 120, 0) { input.moveForward = it }
        holdButton(root, "↓", start, 80, 20, 0) { input.moveBack = it }
        holdButton(root, "←", start, 20, 70, 0) { input.moveLeft = it }
        holdButton(root, "→", start, 140, 70, 0) { input.moveRight = it }
        clickButton(root, "⛏", end, 0, 120, 100) { input.breakRequested = true }
        clickButton(root, "▣", end, 0, 120, 20) { input.placeRequested = true }
        holdButton(root, "⤒", end, 0, 20, 20) { input.jump = it }

        setContentView(root)

        handler.post(hotbarUpdater)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun buildHotbar(root: FrameLayout) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL

        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        lp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        lp.bottomMargin = dp(16)
        row.layoutParams = lp

        for (i in 1..3) {
            val b = Button(this)
            b.textSize = 13f
            val p = LinearLayout.LayoutParams(dp(96), dp(64))
            p.marginEnd = dp(6)
            b.layoutParams = p
            val index = i
            b.setOnClickListener {
                gameView.input.selectedSlot = index
                refreshHotbar()
            }
            row.addView(b)
            hotbarButtons[i] = b
        }

        root.addView(row)
    }

    private fun refreshHotbar() {
        val inv = gameView.input.inventory
        val sel = gameView.input.selectedSlot
        for (i in 1..3) {
            val b = hotbarButtons[i] ?: continue
            val count = inv.get(i)
            val name = blockNames[i]
            b.text = "$name\n$count"
            b.alpha = if (sel == i) 1.0f else 0.6f
            b.setBackgroundColor(
                if (sel == i) 0xAAFFFFFF.toInt() else 0x55FFFFFF
            )
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun holdButton(
        root: FrameLayout,
        text: String,
        gravity: Int,
        left: Int,
        bottom: Int,
        right: Int,
        onHold: (Boolean) -> Unit
    ) {
        val b = Button(this)
        b.text = text
        b.textSize = 24f
        b.alpha = 0.5f
        val size = dp(56)
        val lp = FrameLayout.LayoutParams(size, size)
        lp.gravity = gravity
        lp.leftMargin = dp(left)
        lp.rightMargin = dp(right)
        lp.bottomMargin = dp(bottom)
        b.layoutParams = lp
        b.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    onHold(true)
                    v.isPressed = true
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    onHold(false)
                    v.isPressed = false
                    true
                }
                else -> true
            }
        }
        root.addView(b)
    }

    private fun clickButton(
        root: FrameLayout,
        text: String,
        gravity: Int,
        left: Int,
        bottom: Int,
        right: Int,
        onClick: () -> Unit
    ) {
        val b = Button(this)
        b.text = text
        b.textSize = 24f
        b.alpha = 0.5f
        val size = dp(56)
        val lp = FrameLayout.LayoutParams(size, size)
        lp.gravity = gravity
        lp.leftMargin = dp(left)
        lp.rightMargin = dp(right)
        lp.bottomMargin = dp(bottom)
        b.layoutParams = lp
        b.setOnClickListener { onClick() }
        root.addView(b)
    }
}
