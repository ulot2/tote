package app.tote.feature.autofill

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.slice.Slice
import android.content.Context
import android.content.res.Configuration
import android.graphics.BlendMode
import android.graphics.drawable.Icon
import android.os.Build
import android.service.autofill.InlinePresentation
import android.widget.RemoteViews
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.RequiresApi
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.v1.InlineSuggestionUi
import app.tote.core.model.Entry
import app.tote.core.model.ValueFormatter

/** Builds the dropdown rows and inline chips for suggestions (Plan §8.5). */
class Presentations(private val context: Context, private val icons: IconRenderer) {

    private val dark: Boolean
        get() = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    private val iconPx: Int get() = (32 * context.resources.displayMetrics.density).toInt()

    fun dropdown(entry: Entry): RemoteViews = RemoteViews(context.packageName, R.layout.tote_autofill_item).apply {
        setImageViewBitmap(R.id.tote_af_icon, icons.badge(entry.icon, dark, iconPx))
        setTextViewText(R.id.tote_af_label, entry.label)
        setTextViewText(R.id.tote_af_value, ValueFormatter.display(entry.type, entry.value))
        setContentDescription(R.id.tote_af_label, "${entry.label}, Tote")
    }

    fun allEntries(): RemoteViews = RemoteViews(context.packageName, R.layout.tote_autofill_all).apply {
        setImageViewBitmap(R.id.tote_af_icon, icons.badge(null, dark, iconPx))
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun inline(entry: Entry, spec: InlinePresentationSpec, attribution: PendingIntent): InlinePresentation? =
        inline(spec, attribution, entry.label, ValueFormatter.display(entry.type, entry.value), icons.badge(entry.icon, dark, iconPx), pinned = false)

    @RequiresApi(Build.VERSION_CODES.R)
    fun inlineAllEntries(spec: InlinePresentationSpec, attribution: PendingIntent): InlinePresentation? =
        inline(spec, attribution, context.getString(R.string.tote_af_all_entries), null, icons.badge(null, dark, iconPx), pinned = false)

    @SuppressLint("RestrictedApi")
    @RequiresApi(Build.VERSION_CODES.R)
    private fun inline(
        spec: InlinePresentationSpec,
        attribution: PendingIntent,
        title: String,
        subtitle: String?,
        icon: android.graphics.Bitmap,
        pinned: Boolean,
    ): InlinePresentation? {
        // Only render if the keyboard supports the v1 inline UI; otherwise the dropdown covers it.
        if (!UiVersions.getVersions(spec.style).contains(UiVersions.INLINE_UI_VERSION_1)) return null
        val content = InlineSuggestionUi.newContentBuilder(attribution)
            .setTitle(title)
            .apply { if (subtitle != null) setSubtitle(subtitle) }
            // Keyboards tint chip icons (Gboard paints them white), which turns a coloured badge into a
            // blank square. DST blend mode tells the renderer to keep the bitmap's own colours.
            .setStartIcon(Icon.createWithBitmap(icon).setTintBlendMode(BlendMode.DST))
            .setContentDescription(if (subtitle != null) "$title, $subtitle" else title)
            .build()
        val slice: Slice = content.slice
        return InlinePresentation(slice, spec, pinned)
    }
}
