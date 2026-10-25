
package com.example.sampleapp

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {
    private val bg = Color.rgb(8, 11, 10)
    private val panel = Color.rgb(17, 24, 19)
    private val neon = Color.rgb(185, 242, 124)
    private val muted = Color.rgb(155, 169, 159)

    private val apiUrl = "https://detnolugerkqfinwvstt.supabase.co"
    private val apiKey = "sb_publishable_turDf0jb5YO33DrUx3k-hA_Oj88A7Oh"

    private lateinit var page: LinearLayout
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg

        if (getSharedPreferences("nexa", MODE_PRIVATE)
                .getString("token", null) != null) {
            showHome()
        } else {
            showLogin()
        }
    }

    private fun makePage(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 35, 24, 24)
            setBackgroundColor(bg)
        }
    }

    private fun label(value: String, size: Float, color: Int): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            setPadding(8, 12, 8, 12)
        }
    }

    private fun field(hintText: String, secret: Boolean = false): EditText {
        return EditText(this).apply {
            hint = hintText
            setHintTextColor(muted)
            setTextColor(Color.WHITE)
            textSize = 16f
            setSingleLine(true)
            if (secret) {
                inputType = 129
            } else {
                inputType = 33
            }
            setPadding(16, 12, 16, 12)
            setBackgroundColor(panel)
            val params = LinearLayout.LayoutParams(
                -1, -2
            )
            params.setMargins(0, 7, 0, 7)
            layoutParams = params
        }
    }

    private fun action(
        value: String,
        callback: () -> Unit
    ): TextView {
        return label(value, 16f, neon).apply {
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setBackgroundColor(panel)
            setOnClickListener { callback() }
            val params = LinearLayout.LayoutParams(-1, -2)
            params.setMargins(0, 7, 0, 7)
            layoutParams = params
        }
    }

    private fun showLogin() {
        page = makePage()
        page.addView(label("NEXA", 34f, neon))
        page.addView(label("پیام‌رسان تو؛ ارتباطی ساده و امن", 15f, muted))
        page.addView(label("ورود یا ساخت حساب", 22f, Color.WHITE))

        email = field("ایمیل", false)
        password = field("رمز عبور (حداقل ۶ کاراکتر)", true)
        page.addView(email)
        page.addView(password)

        status = label("", 14f, neon)
        page.addView(status)

        page.addView(action("ورود به حساب") {
            authenticate(false)
        })
        page.addView(action("ساخت حساب جدید") {
            authenticate(true)
        })
        page.addView(label(
            "برای ساخت حساب، از ایمیل واقعی استفاده کن.",
            13f, muted
        ))
        setContentView(page)
    }

    private fun authenticate(register: Boolean) {
        val mail = email.text.toString().trim()
        val pass = password.text.toString()

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            status.text = "ایمیل را درست وارد کن."
            return
        }
        if (pass.length < 6) {
            status.text = "رمز عبور باید حداقل ۶ کاراکتر باشد."
            return
        }

        status.text = "در حال اتصال امن به NEXA..."

        Thread {
            try {
                val endpoint = if (register) {
                    "$apiUrl/auth/v1/signup"
                } else {
                    "$apiUrl/auth/v1/token?grant_type=password"
                }

                val body = JSONObject().apply {
                    put("email", mail)
                    put("password", pass)
                }

                val response = request(endpoint, body)
                val json = JSONObject(response)
                val token = json.optString("access_token")
                val user = json.optJSONObject("user")
                val userEmail = user?.optString("email", mail) ?: mail

                runOnUiThread {
                    if (token.isNotEmpty()) {
                        getSharedPreferences("nexa", MODE_PRIVATE)
                            .edit()
                            .putString("token", token)
                            .putString("email", userEmail)
                            .apply()
                        showHome()
                    } else if (register) {
                        status.text =
                            "حساب درخواست شد. ایمیلت را برای تأیید بررسی کن، سپس وارد شو."
                    } else {
                        status.text = "ورود انجام نشد؛ دوباره تلاش کن."
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "خطا: ${e.message ?: "اتصال برقرار نشد"}"
                }
            }
        }.start()
    }

    private fun request(endpoint: String, body: JSONObject): String {
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.doOutput = true
        connection.setRequestProperty("apikey", apiKey)
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")

        connection.outputStream.use {
            it.write(body.toString().toByteArray(Charsets.UTF_8))
        }

        val code = connection.responseCode
        val stream = if (code in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        val result = stream?.bufferedReader()?.use { it.readText() } ?: ""
        connection.disconnect()

        if (code !in 200..299) {
            val message = try {
                val error = JSONObject(result)
                error.optString(
                    "msg",
                    error.optString(
                        "message",
                        error.optString("error_description", "درخواست ناموفق بود")
                    )
                )
            } catch (_: Exception) {
                "درخواست ناموفق بود (HTTP $code)"
            }
            throw Exception(message)
        }
        return result
    }

    private fun showHome() {
        val mail = getSharedPreferences("nexa", MODE_PRIVATE)
            .getString("email", "") ?: ""

        page = makePage()
        page.addView(label("NEXA", 34f, neon))
        page.addView(label("خوش آمدی!", 25f, Color.WHITE))
        page.addView(label(mail, 15f, muted))
        page.addView(label(
            "حساب تو به Supabase متصل شده است.",
            16f, neon
        ))
        page.addView(label(
            "مرحله بعد: ساخت گفت‌وگوهای واقعی و ارسال پیام بین کاربران.",
            15f, Color.WHITE
        ))
        page.addView(action("خروج از حساب") {
            getSharedPreferences("nexa", MODE_PRIVATE)
                .edit().clear().apply()
            showLogin()
        })
        setContentView(page)
    }
}
