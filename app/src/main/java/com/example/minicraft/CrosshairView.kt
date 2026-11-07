package com.example.minicraft

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class CrosshairView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 4f
        style = Paint.Style.STROKE
        setShadowLayer(2f, 0f, 0f, Color.BLACK)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val arm = 16f
        val gap = 6f

        // горизонтальные линии
        canvas.drawLine(cx - gap - arm, cy, cx - gap, cy, paint)
        canvas.drawLine(cx + gap, cy, cx + gap + arm, cy, paint)

        // вертикальные линии
        canvas.drawLine(cx, cy - gap - arm, cx, cy - gap, paint)
        canvas.drawLine(cx, cy + gap, cx, cy + gap + arm, paint)
    }
}
