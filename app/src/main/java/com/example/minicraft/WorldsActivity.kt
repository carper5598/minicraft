package com.example.minicraft

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class WorldsActivity : Activity() {

    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.rgb(40, 60, 90))
        root.setPadding(dp(16), dp(16), dp(16), dp(16))

        val header = TextView(this)
        header.text = "Миры"
        header.textSize = 22f
        header.setTextColor(Color.WHITE)
        root.addView(header)

        val create = Button(this)
        create.text = "Создать мир"
        create.setOnClickListener { showCreateDialog() }
        root.addView(create)

        val scroll = ScrollView(this)
        list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        setContentView(root)
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        list.removeAllViews()
        val worlds = WorldStore.list(this)
        if (worlds.isEmpty()) {
            val empty = TextView(this)
            empty.text = "Пока нет миров. Создай первый!"
            empty.setTextColor(Color.LTGRAY)
            list.addView(empty)
            return
        }
        for (w in worlds) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(0, dp(8), 0, dp(8))

            val open = Button(this)
            open.text = w.name
            open.layoutParams = LinearLayout.LayoutParams(0, dp(52), 1f)
            open.setOnClickListener {
                val i = Intent(this, GameActivity::class.java)
                i.putExtra("worldPath", w.file.absolutePath)
                i.putExtra("worldName", w.name)
                startActivity(i)
            }
            row.addView(open)

            val del = Button(this)
            del.text = "✕"
            del.layoutParams = LinearLayout.LayoutParams(dp(52), dp(52))
            del.setOnClickListener { confirmDelete(w) }
            row.addView(del)

            list.addView(row)
        }
    }

    private fun showCreateDialog() {
        val input = EditText(this)
        input.hint = "Название мира"
        val dialog = AlertDialog.Builder(this)
            .setTitle("Новый мир")
            .setView(input)
            .setPositiveButton("Создать") { _, _ ->
                val name = input.text.toString()
                val created = WorldStore.create(this, name)
                if (created == null) {
                    AlertDialog.Builder(this)
                        .setTitle("Ошибка")
                        .setMessage("Не удалось создать мир (пустое или занятое имя).")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    refresh()
                }
            }
            .setNegativeButton("Отмена", null)
            .create()
        dialog.show()
    }

    private fun confirmDelete(w: WorldInfo) {
        AlertDialog.Builder(this)
            .setTitle("Удалить мир?")
            .setMessage("Мир «${w.name}» будет удалён безвозвратно.")
            .setPositiveButton("Удалить") { _, _ ->
                WorldStore.delete(w)
                refresh()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
