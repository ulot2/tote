package app.tote.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import app.tote.feature.autofill.IconRenderer
import app.tote.feature.autofill.Presentations
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders the autofill dropdown rows (RemoteViews) to PNGs, as the system would show them. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DropdownScreenshotTest {

    private fun render(name: String) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val p = Presentations(context, IconRenderer(context))
        val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        listOf(SampleData.linkedin, SampleData.portfolio, SampleData.github).forEach { column.addView(p.dropdown(it).apply(context, column)) }
        column.addView(p.allEntries().apply(context, column))
        val w = (360 * context.resources.displayMetrics.density).toInt()
        column.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        column.layout(0, 0, column.measuredWidth, column.measuredHeight)
        val bmp = Bitmap.createBitmap(column.measuredWidth, column.measuredHeight, Bitmap.Config.ARGB_8888)
        column.draw(Canvas(bmp))
        val dir = File(System.getProperty("user.dir"), "build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test @Config(qualifiers = "night-xxhdpi") fun dropdownDark() = render("autofill-dropdown-dark")
    @Test @Config(qualifiers = "notnight-xxhdpi") fun dropdownLight() = render("autofill-dropdown-light")
}
