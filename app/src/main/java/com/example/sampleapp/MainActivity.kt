package com.example.sampleapp

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

    private val apiUrl =
        "https://detnolugerkqfinwvstt.supabase.co"

    private val apiKey =
        "sb_publishable_turDf0jb5YO33DrUx3k-hA_Oj88A7Oh"

    private var token = ""
    private var myId = ""
    private var myEmail = ""
    private var chatId = ""
    private var chatName = ""

    private lateinit var page: LinearLayout
    private lateinit var status: TextView
    private lateinit var messageInput: EditText

    private val handler = Handler(Looper.getMainLooper())
    private var refreshRunnable: Runnable? = null
    private var screen = "login"

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

    override fun onDestroy() {
        stopAutoRefresh()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBackPressed() {
        when (screen) {
            "chat" -> {
                stopAutoRefresh()
                loadConversations()
            }
            "conversations" -> loadProfileAndUsers()
            else -> super.onBackPressed()
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
                    if (::status.isInitialized) {
                        status.text =
                            "خطا: ${e.message ?: "اتصال ناموفق"}"
                    } else {
                        showLogin()
                    }
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
        val conn = URL("$apiUrl$path")
            .openConnection() as HttpURLConnection

        try {
            conn.requestMethod = method
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.setRequestProperty("apikey", apiKey)
            conn.setRequestProperty("Accept", "application/json")

            if (authenticated && token.isNotEmpty()) {
                conn.setRequestProperty(
                    "Authorization",
                    "Bearer $token"
                )
            }

            if (prefer != null) {
                conn.setRequestProperty("Prefer", prefer)
            }

            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                conn.outputStream.use {
                    it.write(body.toByteArray(Charsets.UTF_8))
                }
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
                    val json = JSONObject(result)
                    json.optString(
                        "message",
                        json.optString(
                            "msg",
                            json.optString(
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
        stopAutoRefresh()
        screen = "login"

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
            authenticate(
                email.text.toString().trim(),
                password.text.toString(),
                false
            )
        })

        page.addView(action("ساخت حساب جدید") {
            authenticate(
                email.text.toString().trim(),
                password.text.toString(),
                true
            )
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
                        "حساب ساخته شد؛ اگر تأیید ایمیل لازم است، ایمیلت را تأیید کن و سپس وارد شو."
                    } else {
                        "ورود انجام نشد؛ اطلاعات را بررسی کن."
                    }
                }
            }
        }
    }

    private fun loadProfileAndUsers() {
        runTask {
            val user = JSONObject(request("/auth/v1/user"))
            myId = user.optString("id")
            myEmail = user.optString("email", myEmail)

            if (myId.isEmpty()) {
                throw Exception("شناسه حساب پیدا نشد.")
            }

            val suffix = myId.replace("-", "").take(6)
            val username = myEmail.substringBefore("@")
                .lowercase()
                .replace(Regex("[^a-z0-9_]"), "_")
                .take(18) + "_" + suffix

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
        stopAutoRefresh()
        screen = "users"

        page = makePage()
        page.addView(label("NEXA", 32f, neon))
        page.addView(label("کاربران پیام‌رسان", 22f))
        page.addView(label(myEmail, 14f, muted))

        status = label("", 14f, neon)
        page.addView(status)

        page.addView(action("به‌روزرسانی فهرست کاربران") {
            loadProfileAndUsers()
        })

        page.addView(action("گفت‌وگوهای من") {
            loadConversations()
        })

        var count = 0

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            val id = user.optString("id")

            if (id.isBlank() || id == myId) continue

            val name = user.optString("display_name")
                .ifBlank {
                    user.optString("username", "کاربر NEXA")
                }

            page.addView(action("●  $name\nشروع گفت‌وگو") {
                openConversation(id, name)
            })

            count++
        }

        if (count == 0) {
            page.addView(
                label(
                    "فعلاً کاربر دیگری ثبت‌نام نکرده است.",
                    15f,
                    muted
                )
            )
            page.addView(
                label(
                    "پس از ثبت‌نام کاربران دیگر، نامشان اینجا نمایش داده می‌شود.",
                    14f,
                    muted
                )
            )
        }

        page.addView(action("خروج از حساب") {
            stopAutoRefresh()
            getSharedPreferences("nexa", MODE_PRIVATE)
                .edit().clear().apply()

            token = ""
            myId = ""
            myEmail = ""
            chatId = ""
            showLogin()
        })

        setContentView(page)
    }

    private fun loadConversations() {
        stopAutoRefresh()
        status.text = "در حال دریافت گفت‌وگوها..."

        runTask {
            val data = JSONArray(
                request(
                    "/rest/v1/conversations" +
                    "?select=id,title,created_by,is_group,created_at" +
                    "&order=created_at.desc"
                )
            )

            runOnUiThread {
                showConversations(data)
            }
        }
    }

    private fun showConversations(data: JSONArray) {
        screen = "conversations"
        page = makePage()

        page.addView(label("NEXA", 32f, neon))
        page.addView(label("گفت‌وگوهای من", 23f))
        page.addView(label(myEmail, 14f, muted))

        status = label("", 14f, neon)
        page.addView(status)

        page.addView(action("به‌روزرسانی گفت‌وگوها") {
            loadConversations()
        })

        page.addView(action("بازگشت به کاربران") {
            loadProfileAndUsers()
        })

        var count = 0

        for (i in 0 until data.length()) {
            val item = data.optJSONObject(i) ?: continue
            val id = item.optString("id")
            if (id.isBlank()) continue

            val title = item.optString("title")
                .ifBlank { "گفت‌وگوی NEXA" }

            page.addView(action("▰  $title\nباز کردن گفت‌وگو") {
                chatId = id
                chatName = title
                showChat()
                loadMessages()
                startAutoRefresh()
            })

            count++
        }

        if (count == 0) {
            page.addView(
                label(
                    "هنوز گفت‌وگویی پیدا نشد.",
                    15f,
                    muted
                )
            )
        }

        setContentView(page)
    }

    private fun openConversation(
        otherId: String,
        otherName: String
    ) {
        if (otherId.isBlank() || otherId == myId) {
            status.text = "انتخاب کاربر نامعتبر است."
            return
        }

        status.text = "در حال بررسی گفت‌وگوی قبلی..."

        runTask {
            // از ساخت دوباره گفت‌وگویی که خودمان
            // با همین نام و همین حساب ساخته‌ایم جلوگیری می‌کنیم.
            val safeTitle = URLEncoder.encode(otherName, "UTF-8")
            val safeMyId = URLEncoder.encode(myId, "UTF-8")

            val existing = JSONArray(
                request(
                    "/rest/v1/conversations" +
                    "?select=id,title,created_by,is_group" +
                    "&created_by=eq.$safeMyId" +
                    "&title=eq.$safeTitle" +
                    "&is_group=eq.false" +
                    "&order=created_at.desc" +
                    "&limit=1"
                )
            )

            var conversationId = ""

            if (existing.length() > 0) {
                conversationId = existing
                    .getJSONObject(0)
                    .optString("id")
            }

            if (conversationId.isBlank()) {
                val conversation = JSONObject().apply {
                    put("created_by", myId)
                    put("is_group", false)
                    put("title", otherName)
                }

                val created = JSONArray(
                    request(
                        "/rest/v1/conversations",
                        "POST",
                        conversation.toString(),
                        true,
                        "return=representation"
                    )
                )

                if (created.length() == 0) {
                    throw Exception("ساخت گفت‌وگو ناموفق بود.")
                }

                conversationId = created
                    .getJSONObject(0)
                    .getString("id")

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
            }

            chatId = conversationId
            chatName = otherName

            runOnUiThread {
                showChat()
                loadMessages()
                startAutoRefresh()
            }
        }
    }
        private lateinit var chatScroll: ScrollView
    private lateinit var messageList: LinearLayout
    private var sendingMessage = false

    private fun showChat() {
        stopAutoRefresh()
        screen = "chat"

        page = makePage()
        page.addView(label("NEXA  /  گفت‌وگو", 26f, neon))
        page.addView(label(chatName, 21f))

        page.addView(action("بازگشت به گفت‌وگوها") {
            stopAutoRefresh()
            loadConversations()
        })

        page.addView(action("به‌روزرسانی پیام‌ها") {
            loadMessages()
        })

        chatScroll = ScrollView(this)

        messageList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(4, 8, 4, 8)
        }

        chatScroll.addView(
            messageList,
          LinearLayout.LayoutParams(
    -1,
    -2
)
        )

        page.addView(
            chatScroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        messageInput = field("پیامت را بنوی...")
        messageInput.maxLines = 4
        messageInput.setSingleLine(false)
        page.addView(messageInput)

        page.addView(action("ارسال پیام") {
            sendMessage()
        })

        status = label("", 13f, neon)
        page.addView(status)

        setContentView(page)
    }

    private fun loadMessages() {
        if (chatId.isBlank() || screen != "chat") return

        val requestedChatId = chatId
        status.text = "در حال دریافت پیام‌ها..."

        runTask {
            val encoded = URLEncoder.encode(
                requestedChatId,
                "UTF-8"
            )

            val data = JSONArray(
                request(
                    "/rest/v1/messages" +
                    "?conversation_id=eq.$encoded" +
                    "&select=id,sender_id,body,created_at" +
                    "&order=created_at.asc" +
                    "&limit=200"
                )
            )

            runOnUiThread {
                // اگر هنگام دریافت پیام‌ها صفحه عوض شده باشد،
                // نتایج گفت‌وگوی قبلی را نمایش نمی‌دهیم.
                if (screen != "chat" || chatId != requestedChatId) {
                    return@runOnUiThread
                }

                val wasNearBottom =
                    chatScroll.getChildAt(0)?.let {
                        val distance =
                            it.bottom - (chatScroll.scrollY +
                                chatScroll.height)
                        distance < 180
                    } ?: true

                messageList.removeAllViews()

                for (i in 0 until data.length()) {
                    val item = data.optJSONObject(i) ?: continue
                    val senderId = item.optString("sender_id")
                    val body = item.optString("body")

                    val mine = senderId == myId

                    val bubble = TextView(this).apply {
                        text = (if (mine) "شما" else chatName) +
                            "\n" + body
                        textSize = 15f
                        setTextColor(
                            if (mine) neon else Color.WHITE
                        )
                        setPadding(14, 10, 14, 10)
                        setBackgroundColor(panel)
                    }

                    val params = LinearLayout.LayoutParams(
                        -1,
                        -2
                    ).apply {
                        setMargins(
                            if (mine) 40 else 4,
                            5,
                            if (mine) 4 else 40,
                            5
                        )
                    }

                    messageList.addView(bubble, params)
                }

                status.text = "${data.length()} پیام"

                if (wasNearBottom || data.length() <= 1) {
                    chatScroll.post {
                        chatScroll.fullScroll(View.FOCUS_DOWN)
                    }
                }
            }
        }
    }

    private fun sendMessage() {
        if (sendingMessage) return

        val text = messageInput.text.toString().trim()

        if (text.isEmpty()) {
            status.text = "اول پیامت را بنویس."
            return
        }

        if (text.length > 5000) {
            status.text = "پیام بیش از حد طولانی است."
            return
        }

        if (chatId.isBlank() || myId.isBlank()) {
            status.text = "گفت‌وگو یا حساب معتبر نیست."
            return
        }

        val destinationChat = chatId

        sendingMessage = true
        status.text = "در حال ارسال..."

        runTask {
            val body = JSONObject().apply {
                put("conversation_id", destinationChat)
                put("sender_id", myId)
                put("body", text)
            }

            request(
                "/rest/v1/messages",
                "POST",
                body.toString(),
                true,
                "return=minimal"
            )

            runOnUiThread {
                sendingMessage = false

                if (screen == "chat" &&
                    chatId == destinationChat) {
                    // فقط پس از ارسال موفق پاک می‌شود.
                    messageInput.text.clear()
                    status.text = "پیام ارسال شد."
                    loadMessages()
                }
            }
        }

        // اگر درخواست خطا بدهد، runTask خطا را نمایش می‌دهد.
        // این بررسی کمک می‌کند دکمه ارسال برای همیشه قفل نماند.
        handler.postDelayed({
            if (sendingMessage) {
                sendingMessage = false
            }
        }, 20000)
    }

    private fun startAutoRefresh() {
        stopAutoRefresh()

        val targetChat = chatId

        refreshRunnable = object : Runnable {
            override fun run() {
                if (screen != "chat" ||
                    chatId != targetChat) {
                    return
                }

                loadMessages()
                handler.postDelayed(this, 5000)
            }
        }

        handler.postDelayed(refreshRunnable!!, 5000)
    }

    private fun stopAutoRefresh() {
        refreshRunnable?.let {
            handler.removeCallbacks(it)
        }
        refreshRunnable = null
    }
}
