package app.tote.ui

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.service.quicksettings.TileService
import androidx.test.core.app.ApplicationProvider
import app.tote.feature.picker.PickerActivity
import app.tote.feature.tile.ToteTileService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The tile is only useful if the system can bind it and the picker it opens resolves. */
@RunWith(RobolectricTestRunner::class)
class TileWiringTest {

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()

    @Test fun tileServiceIsDeclaredForQuickSettings() {
        val services = context.packageManager.queryIntentServices(
            Intent(TileService.ACTION_QS_TILE).setPackage(context.packageName),
            PackageManager.GET_META_DATA,
        )
        val tile = services.single()
        assertEquals(ToteTileService::class.java.name, tile.serviceInfo.name)
        assertEquals("android.permission.BIND_QUICK_SETTINGS_TILE", tile.serviceInfo.permission)
    }

    @Test fun pickerIntentResolvesToPrivatePickerActivity() {
        val intent = PickerActivity.copyIntent(context)
        val info = context.packageManager.getActivityInfo(ComponentName(context, PickerActivity::class.java), 0)
        assertEquals(PickerActivity::class.java.name, intent.component?.className)
        assertTrue("picker must not be exported", !info.exported)
        assertTrue("picker must run in its own task", info.taskAffinity.isNullOrEmpty())
    }

    @Test fun noInternetPermission() {
        val pkg = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        assertTrue(pkg.requestedPermissions.orEmpty().none { it == "android.permission.INTERNET" })
    }
}
