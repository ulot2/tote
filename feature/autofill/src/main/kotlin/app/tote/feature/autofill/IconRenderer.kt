package app.tote.feature.autofill

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import app.tote.core.model.EntryIcon
import app.tote.core.model.EntryIconPalette

/**
 * Draws entry badges as bitmaps for RemoteViews and inline suggestions, which can't use Compose.
 * Same colours and shapes as the in-app badge ([EntryIconPalette]). Bitmaps are cached.
 */
class IconRenderer(private val context: Context) {

    private val cache = HashMap<Triple<EntryIcon?, Boolean, Int>, Bitmap>()

    /** [icon] null renders Tote's own mark (the "All entries…" row). */
    @Synchronized
    fun badge(icon: EntryIcon?, dark: Boolean, sizePx: Int): Bitmap = cache.getOrPut(Triple(icon, dark, sizePx)) {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val colors = if (icon == null) {
            EntryIconPalette.Colors(0xFFC8F23C, 0xFF16200A) // lime tile, ink mark
        } else {
            EntryIconPalette.colors(icon, dark)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.container.toInt() }
        val r = sizePx * 0.3f
        canvas.drawRoundRect(RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()), r, r, paint)

        val monogram = icon?.let(EntryIconPalette::monogram)
        if (monogram != null) {
            paint.color = colors.content.toInt()
            paint.typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            paint.textSize = sizePx * 0.42f
            paint.textAlign = Paint.Align.CENTER
            val y = sizePx / 2f - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(monogram, sizePx / 2f, y, paint)
        } else {
            val drawable = ContextCompat.getDrawable(context, drawableFor(icon))!!.mutate()
            drawable.setTint(colors.content.toInt())
            val fraction = if (icon == null) 0.7f else if (icon.isBrand) 0.46f else 0.52f
            val inset = ((sizePx * (1 - fraction)) / 2).toInt()
            drawable.setBounds(inset, inset, sizePx - inset, sizePx - inset)
            drawable.draw(canvas)
        }
        bitmap
    }

    private fun drawableFor(icon: EntryIcon?): Int = when (icon) {
        null -> R.drawable.ic_tote_mark
        EntryIcon.GITHUB -> R.drawable.ic_brand_github
        EntryIcon.X -> R.drawable.ic_brand_x
        EntryIcon.BEHANCE -> R.drawable.ic_brand_behance
        EntryIcon.DRIBBBLE -> R.drawable.ic_brand_dribbble
        EntryIcon.GOOGLE_DRIVE -> R.drawable.ic_brand_drive
        EntryIcon.GLOBE -> R.drawable.ic_entry_globe
        EntryIcon.MAIL -> R.drawable.ic_entry_mail
        EntryIcon.PHONE -> R.drawable.ic_entry_phone
        EntryIcon.DOC -> R.drawable.ic_entry_doc
        EntryIcon.LOCATION -> R.drawable.ic_entry_location
        EntryIcon.TEXT, EntryIcon.LINKEDIN, EntryIcon.MEDIUM -> R.drawable.ic_entry_text
    }
}
