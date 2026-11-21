package app.tote.feature.playground

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import app.tote.feature.autofill.AutofillDiagnostics
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * A screen of realistic form fields for trying autofill (Plan §11): native EditTexts above, and a
 * local job-application page in a WebView below. Plain Android views on purpose: that's what most
 * apps the user fills look like, and it's what the autofill framework handles best.
 */
class AutofillPlaygroundActivity : ComponentActivity() {

    private val dark get() = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    private val bg get() = if (dark) 0xFF0F0E0C.toInt() else 0xFFF6F3EC.toInt()
    private val fg get() = if (dark) 0xFFF5F2EA.toInt() else 0xFF1B1A16.toInt()
    private val muted get() = if (dark) 0xFFA9A396.toInt() else 0xFF5C584F.toInt()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        title = "Try autofill"

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(24))
        }
        column.addView(text("Try autofill", 26f, fg, bold = true))
        column.addView(text("Tap a field. Tote's suggestions appear in a dropdown or above the keyboard.", 14f, muted).apply { setPadding(0, dp(4), 0, dp(8)) })
        column.addView(status)

        column.addView(section("In an app"))
        column.addView(field("LinkedIn profile"))
        column.addView(field("Website", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI))
        column.addView(field("Email", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS))
        column.addView(field("Phone", InputType.TYPE_CLASS_PHONE))
        column.addView(field("Tell us about yourself", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE))
        column.addView(field("Password (no suggestions here)", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD))

        column.addView(section("On a web page"))
        val web = WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = false
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
            loadUrl("file:///android_asset/playground/job_form.html")
        }
        column.addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
            addView(column)
        }
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
        setContentView(scroll)
    }

    /** Shows what Tote's last fill request did, so a silent failure is visible on the phone. */
    private val status by lazy { text("", 13f, muted).apply { setPadding(0, 0, 0, dp(8)) } }
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() {
            status.text = statusText()
            handler.postDelayed(this, 1_000)
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(refresh)
    }

    override fun onPause() {
        handler.removeCallbacks(refresh)
        super.onPause()
    }

    private fun statusText(): String {
        val enabled = getSystemService(android.view.autofill.AutofillManager::class.java)?.hasEnabledAutofillServices() == true
        if (!enabled) return "Tote isn't your autofill service yet. Turn it on in Tote's Settings."
        val last = AutofillDiagnostics.last ?: return "Waiting for a request: tap a field."
        val ago = ((System.currentTimeMillis() - last.atMillis) / 1000).coerceAtLeast(0)
        return if (last.error != null) {
            "Last request failed (${last.error}), ${ago}s ago. Please report this."
        } else {
            "Last request: ${last.fields} fields, ${last.suggestions} suggestions, ${last.durationMs} ms, ${ago}s ago."
        }
    }

    private fun section(title: String) = text(title.uppercase(), 12f, muted, bold = true).apply {
        letterSpacing = 0.08f
        setPadding(0, dp(20), 0, dp(4))
    }

    private fun field(label: String, type: Int = InputType.TYPE_CLASS_TEXT): View {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(10), 0, 0) }
        // Android leaves a plain TextView out of the structure it sends autofill services unless it
        // has a resource id or is marked important. Real apps' labels usually have ids from XML; this
        // screen builds views in code, so mark the label important to behave the same way.
        box.addView(text(label, 14f, fg, bold = true).apply { importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES })
        box.addView(
            EditText(this).apply {
                // No hint: like many real apps, the only label is the TextView above (Tote's "nearby text").
                id = View.generateViewId()
                inputType = type
                setTextColor(fg)
                setHintTextColor(muted)
                gravity = Gravity.TOP or Gravity.START
                if (type and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0) minLines = 3
            },
        )
        return box
    }

    private fun text(s: String, sp: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = s
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
