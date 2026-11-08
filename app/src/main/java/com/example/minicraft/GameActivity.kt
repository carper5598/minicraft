package com.example.minicraft

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.widget.Button
import android.widget.FrameLayout
import java.io.File

class GameActivity : Activity() {

    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val path = intent.getStringExtra("worldPath")
        if (path == null) { finish(); return }

        val worldFile = File(path)
        gameView = GameView(this, worldFile)

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

        val input = gameView.input
        val start = Gravity.BOTTOM or Gravity.START
        val end = Gravity.BOTTOM or Gravity.END

        // Кнопка "Меню" сверху слева
        val menuBtn = Button(this)
        menuBtn.text = "← Меню"
        menuBtn.textSize = 16f
        menuBtn.alpha = 0.7f
        val menuLp = FrameLayout.LayoutParams(dp(110), dp(48))
        menuLp.gravity = Gravity.TOP or Gravity.START
        menuLp.leftMargin = dp(12)
        menuLp.topMargin = dp(12)
        menuBtn.layoutParams = menuLp
        menuBtn.setOnClickListener {
            // Возврат в список миров (WorldsActivity)
            finish()
        }
        root.addView(menuBtn)

        holdButton(root, "↑", start, 80, 120, 0) { input.moveForward = it }
        holdButton(root, "↓", start, 80, 20, 0) { input.moveBack = it }
        holdButton(root, "←", start, 20, 70, 0) { input.moveLeft = it }
        holdButton(root, "→", start, 140, 70, 0) { input.moveRight = it }
        clickButton(root, "⛏", end, 0, 120, 100) { input.breakRequested = true }
        clickButton(root, "▣", end, 0, 120, 20) { input.placeRequested = true }
        holdButton(root, "⤒", end, 0, 20, 20) { input.jump = it }

        setContentView(root)
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
                MotionEvent.ACTION_DOWN -> { onHold(true); v.isPressed = true; true }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { onHold(false); v.isPressed = false; true }
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
