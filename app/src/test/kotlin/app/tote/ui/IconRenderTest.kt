package app.tote.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import app.tote.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders the launcher icon (adaptive: iris background + mark) and the monochrome/tile glyphs to PNG. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "xxhdpi")
class IconRenderTest {
    @Test fun renderIcons() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val size = 432
        val out = Bitmap.createBitmap(size * 3 + 80, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.rgb(40, 40, 40))
        fun draw(res: Int, x: Int, bg: Int, tint: Int?) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bg }
            canvas.drawRoundRect(RectF(x.toFloat(), 0f, (x + size).toFloat(), size.toFloat()), size * 0.25f, size * 0.25f, p)
            val d = ContextCompat.getDrawable(ctx, res)!!.mutate()
            tint?.let { d.setTint(it) }
            d.setBounds(x, 0, x + size, size)
            d.draw(canvas)
        }
        draw(R.drawable.ic_launcher_foreground, 0, Color.parseColor("#A99BFF"), null)
        draw(R.drawable.ic_launcher_monochrome, size + 40, Color.parseColor("#2C3A48"), Color.parseColor("#C9DDF0"))
        draw(app.tote.feature.tile.R.drawable.ic_tile, size * 2 + 80, Color.parseColor("#3C4043"), Color.WHITE)
        val dir = File(System.getProperty("user.dir"), "build/screenshots").apply { mkdirs() }
        File(dir, "icons.png").outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
