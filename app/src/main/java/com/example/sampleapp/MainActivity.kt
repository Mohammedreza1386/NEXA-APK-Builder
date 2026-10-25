
package com.example.sampleapp

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.EditText

class MainActivity : Activity() {

    private val bg = Color.rgb(8, 11, 10)
    private val panel = Color.rgb(17, 24, 19)
    private val neon = Color.rgb(185, 242, 124)
    private val muted = Color.rgb(155, 169, 159)

    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.setBackgroundColor(bg)
        showHome()
    }

    private fun text(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER_VERTICAL
            if (bold) setTypeface(null, Typeface.BOLD)
            setPadding(18, 14, 18, 14)
        }
    }

    private fun showHome() {
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 24, 18, 18)
            setBackgroundColor(bg)
        }

        content.addView(text("NEXA", 30f, neon, true))
        content.addView(text("ارتباطی ساده، مدرن و هوشمند", 14f, muted))
        content.addView(text("گفتگوها", 21f, Color.WHITE, true))

        val search = EditText(this).apply {
            hint = "جست‌وجوی گفتگوها"
            setHintTextColor(muted)
            setTextColor(Color.WHITE)
            textSize = 15f
            setSingleLine(true)
            setPadding(16, 10, 16, 10)
        }
        content.addView(search)

        val chats = listOf(
            "تیم NEXA" to "به دنیای NEXA خوش آمدی",
            "دوستان" to "گفتگوهای دوستانت",
            "یادداشت‌های من" to "فضای شخصی تو"
        )

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        content.addView(list)

        fun drawChats(query: String = "") {
            list.removeAllViews()
            chats.filter {
                it.first.contains(query, true) ||
                    it.second.contains(query, true)
            }.forEach { item ->
                val row = TextView(this).apply {
                    text = "●   ${item.first}\n      ${item.second}"
                    textSize = 16f
                    setTextColor(neon)
                    setPadding(16, 20, 16, 20)
                    setBackgroundColor(panel)
                    setOnClickListener {
                        showConversation(item.first, item.second)
                    }
                }
                val params = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 8, 0, 8)
                list.addView(row, params)
            }
        }

        drawChats()
        search.addTextChangedListener(object :
            android.text.TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {}
            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) {
                drawChats(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        setContentView(content)
    }

    private fun showConversation(name: String, preview: String) {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 28, 20, 20)
            setBackgroundColor(bg)
        }
        page.addView(text("‹   بازگشت", 17f, neon).apply {
            setOnClickListener { showHome() }
        })
        page.addView(text(name, 25f, neon, true))
        page.addView(text(preview, 15f, Color.WHITE))
        page.addView(text(
            "این نسخه هنوز به سامانه ارسال پیام متصل نیست.",
            13f, muted
        ))
        setContentView(page)
    }
}
