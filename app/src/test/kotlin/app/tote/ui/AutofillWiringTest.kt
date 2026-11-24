package app.tote.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.service.autofill.AutofillService
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import app.tote.core.model.EntryRanking
import app.tote.feature.autofill.AutofillResults
import app.tote.feature.autofill.ToteAutofillService
import app.tote.feature.picker.AutofillPickerActivity
import app.tote.feature.picker.PickerMode
import app.tote.feature.picker.PickerScreen
import app.tote.feature.picker.PickerUiState
import app.tote.ui.theme.ToteTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h1200dp")
class AutofillWiringTest {

    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()

    @Test fun autofillServiceIsDeclaredWithConfig() {
        val services = context.packageManager.queryIntentServices(
            Intent(AutofillService.SERVICE_INTERFACE).setPackage(context.packageName),
            PackageManager.GET_META_DATA,
        )
        val info = services.single().serviceInfo
        assertEquals(ToteAutofillService::class.java.name, info.name)
        assertEquals("android.permission.BIND_AUTOFILL_SERVICE", info.permission)
        assertTrue("needs the android.autofill config", info.metaData?.containsKey("android.autofill") == true)
    }

    @Test fun fillPickerReturnsResultsToTheCallingApp() {
        val info = context.packageManager.getActivityInfo(ComponentName(context, AutofillPickerActivity::class.java), 0)
        assertFalse(info.exported)
        // Standard launch mode and the app's own task affinity: the fill result has to reach the caller.
        assertEquals(ActivityInfo.LAUNCH_MULTIPLE, info.launchMode)
        assertEquals(context.packageName, info.taskAffinity)
    }

    @Test fun fillIntentCarriesTheFieldAndUrlFlag() {
        val id = autofillId(3)
        val intent = AutofillPickerActivity.fillIntent(context, id, urlField = true)
        assertEquals(id, AutofillResults.autofillId(intent))
        assertTrue(intent.getBooleanExtra(AutofillResults.EXTRA_URL_FIELD, false))
    }

    @Test fun fillResultHoldsADataset() {
        val result = AutofillResults.resultFor(context, autofillId(3), SampleData.github, urlField = true)
        assertNotNull(result.extras?.get(AutofillManager.EXTRA_AUTHENTICATION_RESULT))
    }

    @Test fun pickerSaysFillInFillMode() {
        compose.setContent {
            ToteTheme(darkTheme = true) {
                PickerScreen(
                    state = PickerUiState(loading = false, entries = EntryRanking.forPicker(SampleData.all), totalCount = SampleData.all.size),
                    onQueryChange = {}, onPick = {}, onOpenApp = {}, onFinished = {},
                    startVisible = true, mode = PickerMode.FILL,
                )
            }
        }
        compose.onNodeWithText("Fill from Tuck").assertIsDisplayed()
    }

    private fun autofillId(value: Int): AutofillId =
        AutofillId::class.java.getDeclaredConstructor(Int::class.javaPrimitiveType).apply { isAccessible = true }.newInstance(value)
}
