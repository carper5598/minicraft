package com.example.minicraft

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent

class GameView(context: Context) : GLSurfaceView(context) {
    val input = InputState()
    private val renderer = GameRenderer(input)
    private var lastX = 0f
    private var lastY = 0f

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { lastX = event.x; lastY = event.y }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                lastX = event.x
                lastY = event.y
                renderer.addLook(dx, dy)
            }
        }
        return true
    }
}
