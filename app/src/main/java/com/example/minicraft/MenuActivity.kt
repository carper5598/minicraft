package com.example.minicraft

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MenuActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER
        root.setBackgroundColor(Color.rgb(70, 120, 200))

        val title = TextView(this)
        title.text = "Добро пожаловать в MiniCraft"
        title.textSize = 26f
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER
        root.addView(title)

        val play = Button(this)
        play.text = "Играть"
        play.textSize = 20f
        val lp = LinearLayout.LayoutParams(dp(220), dp(56))
        lp.topMargin = dp(40)
        play.layoutParams = lp
        play.setOnClickListener {
            startActivity(Intent(this, WorldsActivity::class.java))
        }
        root.addView(play)

        setContentView(root)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
