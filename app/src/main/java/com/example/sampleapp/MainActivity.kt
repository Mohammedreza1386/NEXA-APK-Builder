package com.example.sampleapp

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MainActivity : Activity() {
    private val bg = Color.rgb(8, 11, 10)
    private val panel = Color.rgb(17, 24, 19)
    private val neon = Color.rgb(185, 242, 124)
    private val muted = Color.rgb(155, 169, 159)

    private val apiUrl = "https://detnolugerkqfinwvstt.supabase.co"
    private val apiKey = "sb_publishable_turDf0jb5YO33DrUx3k-hA_Oj88A7Oh"

    private var token = ""
    private var myId = ""
    private var myEmail = ""
    private var chatId = ""
    private var chatName = ""
    private lateinit var page: LinearLayout
    private lateinit var status: TextView
    private lateinit var messageInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg

        val prefs = getSharedPreferences("nexa", MODE_PRIVATE)
        token = prefs.getString("token", "") ?: ""
        myEmail = prefs.getString("email", "") ?: ""

        if (token.isNotEmpty()) {
    page = makePage()
    status = label("در حال اتصال به NEXA...", 14f, neon)
    page.addView(label("NEXA", 32f, neon))
    page.addView(status)
    setContentView(page)
    loadProfileAndUsers()
} else {
    showLogin()
}
    }

    private fun makePage(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 24, 20, 20)
            setBackgroundColor(bg)
        }
    }

    private fun label(
        value: String,
        size: Float = 16f,
        color: Int = Color.WHITE
    ) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        setPadding(8, 10, 8, 10)
    }

    private fun field(
        hintText: String,
        secret: Boolean = false
    ) = EditText(this).apply {
        hint = hintText
        setHintTextColor(muted)
        setTextColor(Color.WHITE)
        textSize = 16f
        setSingleLine(true)
        inputType = if (secret) 129 else 33
        setPadding(14, 12, 14, 12)
        setBackgroundColor(panel)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(0, 6, 0, 6)
        }
    }

    private fun action(
        value: String,
        callback: () -> Unit
    ) = TextView(this).apply {
        text = value
        textSize = 16f
        setTextColor(neon)
        gravity = Gravity.CENTER
        typeface = Typeface.DEFAULT_BOLD
        setPadding(12, 14, 12, 14)
        setBackgroundColor(panel)
        setOnClickListener { callback() }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(0, 5, 0, 5)
        }
    }

    private fun runTask(task: () -> Unit) {
        Thread {
            try {
                task()
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "خطا: ${e.message ?: "اتصال ناموفق"}"
                }
            }
        }.start()
    }

    private fun request(
        path: String,
        method: String = "GET",
        body: String? = null,
        authenticated: Boolean = true,
        prefer: String? = null
    ): String {
        val conn = (URL("$apiUrl$path").openConnection()
                as HttpURLConnection)

        try {
            conn.requestMethod = method
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.setRequestProperty("apikey", apiKey)
            conn.setRequestProperty("Accept", "application/json")

            if (authenticated && token.isNotEmpty()) {
                conn.setRequestProperty(
                    "Authorization", "Bearer $token"
                )
            }

            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty(
                    "Content-Type", "application/json"
                )
                conn.outputStream.use {
                    it.write(body.toByteArray(Charsets.UTF_8))
                }
            }

            if (prefer != null) {
                conn.setRequestProperty("Prefer", prefer)
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) {
                conn.inputStream
            } else {
                conn.errorStream
            }

            val result = stream?.bufferedReader()
                ?.use { it.readText() } ?: ""

            if (code !in 200..299) {
                val message = try {
                    val j = JSONObject(result)
                    j.optString(
                        "message",
                        j.optString(
                            "msg",
                            j.optString(
                                "error_description",
                                "HTTP $code"
                            )
                        )
                    )
                } catch (_: Exception) {
                    "HTTP $code: $result"
                }
                throw Exception(message)
            }

            return result
        } finally {
            conn.disconnect()
        }
    }

    private fun showLogin() {
        page = makePage()
        page.addView(label("NEXA", 34f, neon))
        page.addView(label("پیام‌رسان تو", 16f, muted))
        page.addView(label("ورود یا ساخت حساب", 22f))

        val email = field("ایمیل")
        val password = field("رمز عبور", true)
        status = label("", 14f, neon)

        page.addView(email)
        page.addView(password)
        page.addView(status)

        page.addView(action("ورود به حساب") {
            authenticate(email.text.toString().trim(),
                password.text.toString(), false)
        })

        page.addView(action("ساخت حساب جدید") {
            authenticate(email.text.toString().trim(),
                password.text.toString(), true)
        })

        setContentView(page)
    }

    private fun authenticate(
        mail: String,
        pass: String,
        register: Boolean
    ) {
        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(mail).matches()) {
            status.text = "ایمیل را درست وارد کن."
            return
        }

        if (pass.length < 6) {
            status.text = "رمز عبور باید حداقل ۶ کاراکتر باشد."
            return
        }

        status.text = "در حال اتصال..."

        runTask {
            val path = if (register) {
                "/auth/v1/signup"
            } else {
                "/auth/v1/token?grant_type=password"
            }

            val body = JSONObject().apply {
                put("email", mail)
                put("password", pass)
            }

            val response = JSONObject(
                request(path, "POST", body.toString(), false)
            )

            val newToken = response.optString("access_token")
            val user = response.optJSONObject("user")

            if (newToken.isNotEmpty()) {
                token = newToken
                myEmail = user?.optString("email", mail) ?: mail

                getSharedPreferences("nexa", MODE_PRIVATE)
                    .edit()
                    .putString("token", token)
                    .putString("email", myEmail)
                    .apply()

                loadProfileAndUsers()
            } else {
                runOnUiThread {
                    status.text = if (register) {
                        "حساب ساخته شد؛ ایمیلت را تأیید کن و سپس وارد شو."
                    } else {
                        "ورود انجام نشد."
                    }
                }
            }
        }
    }

    private fun loadProfileAndUsers() {
        runTask {
            val user = JSONObject(
                request("/auth/v1/user")
            )
            myId = user.optString("id")
            myEmail = user.optString("email", myEmail)

            if (myId.isEmpty()) {
                throw Exception("شناسه حساب پیدا نشد.")
            }

            val suffix = myId.replace("-", "").take(6)
            val username = (
                myEmail.substringBefore("@")
                    .lowercase()
                    .replace(Regex("[^a-z0-9_]"), "_")
                    .take(18) + "_" + suffix
                )

            val profile = JSONObject().apply {
                put("id", myId)
                put("username", username)
                put("display_name", myEmail.substringBefore("@"))
            }

            request(
                "/rest/v1/profiles?on_conflict=id",
                "POST",
                profile.toString(),
                true,
                "resolution=merge-duplicates,return=minimal"
            )

            val users = JSONArray(
                request(
                    "/rest/v1/profiles" +
                    "?select=id,username,display_name" +
                    "&order=created_at.desc"
                )
            )

            runOnUiThread {
                showUsers(users)
            }
        }
    }

    private fun showUsers(users: JSONArray) {
        page = makePage()
        page.addView(label("NEXA", 32f, neon))
        page.addView(label("کاربران پیام‌رسان", 22f))
        page.addView(label(myEmail, 14f, muted))

        status = label("", 14f, neon)
        page.addView(status)

        page.addView(action("به‌روزرسانی فهرست") {
            loadProfileAndUsers()
        })

        var count = 0

        for (i in 0 until users.length()) {
            val u = users.getJSONObject(i)
            val id = u.optString("id")
            if (id == myId) continue

            val name = u.optString("display_name")
                .ifBlank { u.optString("username", "کاربر NEXA") }

            page.addView(action("●  $name\nشروع گفت‌وگو") {
                openConversation(id, name)
            })
            count++
        }

        if (count == 0) {
            page.addView(label(
                "فعلاً کاربر دیگری ثبت‌نام نکرده است.",
                15f, muted
            ))
            page.addView(label(
                "برای آزمایش پیام‌رسان، حساب دوم را با ایمیل دیگری بساز.",
                14f, muted
            ))
        }

        page.addView(action("خروج از حساب") {
            getSharedPreferences("nexa", MODE_PRIVATE)
                .edit().clear().apply()
            token = ""
            myId = ""
            showLogin()
        })

        setContentView(page)
    }

    private fun openConversation(
        otherId: String,
        otherName: String
    ) {
        status.text = "در حال ساخت گفت‌وگو..."

        runTask {
            val conversation = JSONObject().apply {
                put("created_by", myId)
                put("is_group", false)
                put("title", otherName)
            }

            val result = JSONArray(
                request(
                    "/rest/v1/conversations",
                    "POST",
                    conversation.toString(),
                    true,
                    "return=representation"
                )
            )

            val conversationId = result
                .getJSONObject(0).getString("id")

            val members = JSONArray()
            members.put(JSONObject().apply {
                put("conversation_id", conversationId)
                put("user_id", myId)
            })
            members.put(JSONObject().apply {
                put("conversation_id", conversationId)
                put("user_id", otherId)
            })

            request(
                "/rest/v1/conversation_members",
                "POST",
                members.toString(),
                true,
                "return=minimal"
            )

            chatId = conversationId
            chatName = otherName

            runOnUiThread {
                showChat()
                loadMessages()
            }
        }
    }

    private fun showChat() {
        page = makePage()
        page.addView(label("NEXA  /  گفت‌وگو", 26f, neon))
        page.addView(label(chatName, 21f))

        page.addView(action("بازگشت به کاربران") {
            loadProfileAndUsers()
        })

        page.addView(action("به‌روزرسانی پیام‌ها") {
            loadMessages()
        })

        val scroll = ScrollView(this)
        val messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        messages.tag = "messages"
        scroll.addView(messages)

        page.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        messageInput = field("پیامت را بنویس...")
        page.addView(messageInput)

        page.addView(action("ارسال پیام") {
            sendMessage()
        })

        status = label("", 13f, neon)
        page.addView(status)

        setContentView(page)
    }

    private fun loadMessages() {
        if (chatId.isEmpty()) return

        runTask {
            val encoded = URLEncoder.encode(chatId, "UTF-8")
            val data = JSONArray(
                request(
                    "/rest/v1/messages" +
                    "?conversation_id=eq.$encoded" +
                    "&select=id,sender_id,body,created_at" +
                    "&order=created_at.asc"
                )
            )

            runOnUiThread {
                val scroll = page.getChildAt(3) as? ScrollView
                    ?: return@runOnUiThread
                val list = scroll.getChildAt(0) as? LinearLayout
                    ?: return@runOnUiThread

                list.removeAllViews()

                for (i in 0 until data.length()) {
                    val m = data.getJSONObject(i)
                    val mine = m.optString("sender_id") == myId
                    val text = m.optString("body")

                    list.addView(label(
                        (if (mine) "شما: " else "$chatName: ") + text,
                        15f,
                        if (mine) neon else Color.WHITE
                    ).apply {
                        setBackgroundColor(panel)
                    })
                }

                status.text = "${data.length()} پیام"
            }
        }
    }

    private fun sendMessage() {
        val text = messageInput.text.toString().trim()

        if (text.isEmpty()) return
        if (text.length > 5000) {
            status.text = "پیام بیش از حد طولانی است."
            return
        }

        val body = JSONObject().apply {
            put("conversation_id", chatId)
            put("sender_id", myId)
            put("body", text)
        }

        status.text = "در حال ارسال..."

        runTask {
            request(
                "/rest/v1/messages",
                "POST",
                body.toString(),
                true,
                "return=minimal"
            )

            runOnUiThread {
                messageInput.text.clear()
                status.text = "پیام ارسال شد."
                loadMessages()
            }
        }
    }
}
