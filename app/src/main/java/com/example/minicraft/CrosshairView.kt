package com.example.minicraft

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class CrosshairView(context: Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 4f
        style = Paint.Style.STROKE
        setShadowLayer(2f, 0f, 0f, Color.BLACK)
    }

    init {
        isClickable = false
        isFocusable = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val arm = 16f
        val gap = 6f
        canvas.drawLine(cx - gap - arm, cy, cx - gap, cy, p)
        canvas.drawLine(cx + gap, cy, cx + gap + arm, cy, p)
        canvas.drawLine(cx, cy - gap - arm, cx, cy - gap, p)
        canvas.drawLine(cx, cy + gap, cx, cy + gap + arm, p)
    }
}
