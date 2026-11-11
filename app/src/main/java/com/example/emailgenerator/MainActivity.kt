package com.example.emailgenerator

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("emailgen", MODE_PRIVATE) }
    private val contacts = mutableListOf<String>()
    private lateinit var contactView: TextView
    private lateinit var statusView: TextView
    private val pickCsv = 41

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadContacts()
        buildUi()
    }

    private fun buildUi() {
        val scroll = ScrollView(this)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 30, 36, 36)
            setBackgroundColor(0xFFF4F7FA.toInt())
        }
        scroll.addView(page)
        setContentView(scroll)

        val header = TextView(this).apply {
            text = "Email Generator\nمولّد وإدارة الإيميلات"
            textSize = 25f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(18, 28, 18, 28)
            setBackgroundColor(0xFF12304A.toInt())
        }
        page.addView(header, matchWrap())

        addSpace(page, 18)
        section(page, "1) إنشاء عناوين تجريبية")
        val firstName = edit(page, "الاسم الأول (مثال: Mohammad)")
        val count = edit(page, "عدد الأمثلة (1 إلى 100)")
        count.setText("20")
        button(page, "توليد عناوين تجريبية", 0xFF1769AA.toInt()) {
            val name = firstName.text.toString().trim().lowercase(Locale.ROOT)
                .replace(Regex("[^a-z0-9]"), "")
            val n = count.text.toString().toIntOrNull()?.coerceIn(1, 100) ?: 20
            if (name.isBlank()) {
                toast("أدخل الاسم الأول بحروف إنجليزية")
            } else {
                val samples = (1..n).joinToString("\n") { "$name${it.toString().padStart(2, '0')}@example.com" }
                statusView.text = "هذه عناوين تجريبية فقط وليست صناديق بريد حقيقية:\n\n$samples"
            }
        }

        addSpace(page, 20)
        section(page, "2) قائمة المشتركين بموافقتهم")
        val emailInput = edit(page, "أدخل البريد الإلكتروني")
        button(page, "إضافة مشترك بموافقته", 0xFF23834A.toInt()) {
            val email = emailInput.text.toString().trim().lowercase(Locale.ROOT)
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                toast("تحقق من عنوان البريد الإلكتروني")
            } else if (contacts.contains(email)) {
                toast("هذا العنوان موجود بالفعل")
            } else {
                contacts.add(email)
                saveContacts()
                emailInput.text.clear()
                renderContacts()
                toast("تمت إضافة العنوان")
            }
        }
        button(page, "استيراد ملف CSV", 0xFF526779.toInt()) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/csv", "text/comma-separated-values", "text/plain"))
            }
            startActivityForResult(intent, pickCsv)
        }
        TextView(this).apply {
            text = "ملف CSV: ضع البريد الإلكتروني في العمود الأول، وعبارة yes أو true في العمود الثاني لتأكيد الموافقة. مثال: name@email.com,yes"
            textSize = 13f
            setTextColor(0xFF526779.toInt())
            setPadding(0, 6, 0, 12)
        }.also { page.addView(it) }

        contactView = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFF203040.toInt())
            setPadding(14, 14, 14, 14)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        page.addView(contactView, matchWrap())
        renderContacts()

        button(page, "حذف جميع العناوين", 0xFFB33A3A.toInt()) {
            contacts.clear()
            saveContacts()
            renderContacts()
        }

        addSpace(page, 20)
        section(page, "3) تجهيز إعلان للمشتركين")
        val subject = edit(page, "عنوان الإعلان")
        val message = EditText(this).apply {
            hint = "اكتب نص الإعلان هنا"
            textSize = 16f
            minLines = 4
            gravity = Gravity.TOP
            setPadding(14, 14, 14, 14)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        page.addView(message, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 8 })
        button(page, "فتح تطبيق البريد لتجهيز الإعلان", 0xFF1769AA.toInt()) {
            if (contacts.isEmpty()) {
                toast("أضف مشتركين وافقوا على استقبال الرسائل أولاً")
            } else if (subject.text.isBlank() || message.text.isBlank()) {
                toast("أدخل عنوان الإعلان ونصه")
            } else {
                val batch = contacts.take(20).toTypedArray()
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_BCC, batch)
                    putExtra(Intent.EXTRA_SUBJECT, subject.text.toString())
                    putExtra(Intent.EXTRA_TEXT, message.text.toString() +
                        "\n\nلإلغاء الاشتراك، يرجى الرد بكلمة إلغاء الاشتراك.")
                }
                try {
                    startActivity(Intent.createChooser(intent, "اختر تطبيق البريد"))
                } catch (_: Exception) {
                    toast("لا يوجد تطبيق بريد مثبت")
                }
                if (contacts.size > 20) toast("تم تجهيز أول 20 عنواناً فقط. أرسل على دفعات لتجنب الإرسال غير المرغوب.")
            }
        }

        statusView = TextView(this).apply {
            text = "جاهز"
            textSize = 13f
            setTextColor(0xFF526779.toInt())
            setPadding(0, 16, 0, 0)
        }
        page.addView(statusView)
        addSpace(page, 24)
        TextView(this).apply {
            text = "تنبيه: لا تستخدم العناوين التجريبية للإرسال. أرسل فقط إلى أشخاص وافقوا على استقبال إعلاناتك، واحترم طلبات إلغاء الاشتراك."
            textSize = 12f
            setTextColor(0xFF7A4B20.toInt())
        }.also { page.addView(it) }
    }

    private fun loadContacts() {
        val arr = JSONArray(prefs.getString("contacts", "[]"))
        for (i in 0 until arr.length()) contacts.add(arr.getString(i))
    }

    private fun saveContacts() {
        val arr = JSONArray()
        contacts.forEach { arr.put(it) }
        prefs.edit().putString("contacts", arr.toString()).apply()
    }

    private fun renderContacts() {
        if (!::contactView.isInitialized) return
        contactView.text = if (contacts.isEmpty()) "لا توجد عناوين محفوظة." else
            "عدد المشتركين: ${contacts.size}\n\n" + contacts.take(100).joinToString("\n")
                + if (contacts.size > 100) "\n..." else ""
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickCsv && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { lines ->
                    lines.forEach { line ->
                        val cols = line.split(",").map { it.trim().trim('"') }
                        if (cols.size >= 2 && (cols[1].equals("yes", true) || cols[1].equals("true", true))) {
                            val email = cols[0].lowercase(Locale.ROOT)
                            if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() && !contacts.contains(email)) {
                                contacts.add(email)
                            }
                        }
                    }
                }
                saveContacts()
                renderContacts()
                toast("اكتمل الاستيراد. أضيفت العناوين التي تحمل موافقة yes أو true فقط.")
            } catch (_: Exception) {
                toast("تعذر قراءة الملف. استخدم CSV نصياً.")
            }
        }
    }

    private fun section(parent: LinearLayout, text: String) {
        TextView(this).apply {
            this.text = text
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF12304A.toInt())
            setPadding(0, 4, 0, 8)
        }.also { parent.addView(it) }
    }

    private fun edit(parent: LinearLayout, hintText: String): EditText {
        val e = EditText(this).apply {
            hint = hintText
            textSize = 16f
            singleLine = true
            setPadding(14, 10, 14, 10)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        parent.addView(e, LinearParams().apply { topMargin = 6 })
        return e
    }

    private fun button(parent: LinearLayout, label: String, color: Int, action: () -> Unit) {
        val b = Button(this).apply {
            text = label
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(color)
            setOnClickListener { action() }
        }
        parent.addView(b, LinearParams().apply { topMargin = 9 })
    }

    private fun addSpace(parent: LinearLayout, h: Int) {
        parent.addView(View(this), LinearLayout.LayoutParams(1, h))
    }

    private fun matchWrap() = LinearLayout.LayoutParams(-1, -2)
    private fun LinearParams() = LinearLayout.LayoutParams(-1, -2)
    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
