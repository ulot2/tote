package app.tote.feature.autofill

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.view.autofill.AutofillId
import androidx.test.core.app.ApplicationProvider
import app.tote.core.data.platform.PickerIntents
import app.tote.core.matching.FieldMatcher
import app.tote.core.matching.FieldSignals
import app.tote.core.matching.MatchContext
import app.tote.core.model.Category
import app.tote.core.model.Entry
import app.tote.core.model.EntryIcon
import app.tote.core.model.EntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FillResponseFactoryTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val pickerIntents = object : PickerIntents {
        override fun copyIntent(context: Context) = Intent("copy")
        override fun fillIntent(context: Context, autofillId: Parcelable, urlField: Boolean) =
            Intent("fill").setPackage(context.packageName).putExtra("id", autofillId).putExtra("url", urlField)
    }
    private val factory = FillResponseFactory(context, FieldMatcher(), Presentations(context, IconRenderer(context)), pickerIntents)

    private val linkedin = Entry(1, "LinkedIn", "linkedin.com/in/someone", EntryType.LINK, Category.LINKS, EntryIcon.LINKEDIN)
    private val portfolio = Entry(2, "Portfolio", "someone.dev", EntryType.LINK, Category.LINKS, EntryIcon.GLOBE)
    private val email = Entry(3, "Email", "someone@example.com", EntryType.EMAIL, Category.CONTACT, EntryIcon.MAIL)
    private val vault = listOf(linkedin, portfolio, email)

    private fun field(id: String, signals: FieldSignals, focused: Boolean = false) = ParsedField(id, signals, focused)

    @Test fun planPutsFocusedFieldFirstAndSkipsIgnoredOnes() {
        val plans = factory.plan(
            listOf(
                field("pw", FieldSignals(label = "Password", htmlType = "password")),
                field("email", FieldSignals(label = "Email", htmlType = "email")),
                field("li", FieldSignals(label = "LinkedIn Profile"), focused = true),
            ),
            vault,
            MatchContext(),
        )
        assertEquals(listOf("li", "email"), plans.map { it.field.id })
        assertEquals(listOf(linkedin), plans[0].entries.take(1))
        assertEquals(listOf(email), plans[1].entries)
        assertTrue(plans.all { it.allEntries })
    }

    @Test fun loneTextBoxGetsNoFallback() {
        // A chat box: no match, one field, not a web page → nothing at all.
        val chat = factory.plan(listOf(field("msg", FieldSignals(hintText = "Message"), focused = true)), vault, MatchContext())
        assertTrue(chat.isEmpty())
        // The same unmatched field on a form → most used + "All entries…".
        val form = factory.plan(
            listOf(field("a", FieldSignals(label = "Anything else?"), focused = true), field("b", FieldSignals(label = "Email"))),
            vault,
            MatchContext(),
        )
        assertEquals("a", form.first().field.id)
        assertTrue(form.first().allEntries)
    }

    @Test fun urlFieldsAreFlaggedSoLinksGetAScheme() {
        val plan = factory.plan(listOf(field("w", FieldSignals(label = "Website", htmlType = "url"), focused = true)), vault, MatchContext()).single()
        assertTrue(plan.urlField)
    }

    @Test fun buildsOneDatasetPerSuggestionPlusAllEntries() {
        val id = autofillId(7)
        val plans = factory.plan(listOf(ParsedField(id, FieldSignals(label = "LinkedIn Profile"), focused = true)), vault, MatchContext())
        val response = factory.build(plans, inlineRequest = null, responseId = "r1")
        assertNotNull(response)
        val datasets = datasetsOf(response!!)
        val ids = datasets.map { datasetIdOf(it) }
        assertEquals("e:1", ids.first())
        assertEquals(FillResponseFactory.ALL_ENTRIES_ID, ids.last())
    }

    @Test
    @org.robolectric.annotation.Config(sdk = [35])
    fun everyFieldGetsKeyboardChipsNotJustTheFocusedOne() {
        // Android reuses a response when focus moves to another field, and hides the dropdown while
        // the keyboard shows chips, so a field without chips would show nothing (seen on a phone).
        val style = androidx.autofill.inline.UiVersions.newStylesBuilder()
            .addStyle(androidx.autofill.inline.v1.InlineSuggestionUi.newStyleBuilder().build())
            .build()
        val spec = android.widget.inline.InlinePresentationSpec.Builder(android.util.Size(100, 40), android.util.Size(600, 40))
            .setStyle(style).build()
        val request = android.view.inputmethod.InlineSuggestionsRequest.Builder(listOf(spec)).setMaxSuggestionCount(4).build()

        val first = autofillId(1)
        val second = autofillId(2)
        val plans = factory.plan(
            listOf(
                ParsedField(first, FieldSignals(label = "LinkedIn Profile"), focused = true),
                ParsedField(second, FieldSignals(label = "Email", htmlType = "email"), focused = false),
            ),
            vault,
            MatchContext(),
        )
        val datasets = datasetsOf(factory.build(plans, request, "r")!!)
        val withChips = datasets.filter { inlineOf(it) != null }.map { fieldIdsOf(it) }.flatten().toSet()
        assertTrue("first field has chips", first in withChips)
        assertTrue("second field has chips too", second in withChips)
    }

    @Suppress("UNCHECKED_CAST")
    private fun fieldIdsOf(dataset: Dataset): List<AutofillId> =
        Dataset::class.java.getDeclaredMethod("getFieldIds").apply { isAccessible = true }.invoke(dataset) as List<AutofillId>

    private fun inlineOf(dataset: Dataset): Any? =
        Dataset::class.java.getDeclaredMethod("getFieldInlinePresentation", Int::class.javaPrimitiveType).apply { isAccessible = true }.invoke(dataset, 0)

    @Test fun nothingToOfferMeansNoResponse() {
        assertNull(factory.build(emptyList(), null, "r"))
    }

    @Test fun datasetIdsCarryOnlyTheEntryId() {
        assertEquals("e:42", FillResponseFactory.datasetId(linkedin.copy(id = 42)))
        assertEquals(42L, FillResponseFactory.entryIdOf("e:42"))
        assertNull(FillResponseFactory.entryIdOf("all"))
        assertNull(FillResponseFactory.entryIdOf(null))
    }

    @Test fun iconsRenderForEveryIcon() {
        val renderer = IconRenderer(context)
        (EntryIcon.entries + listOf<EntryIcon?>(null)).forEach { icon ->
            val bmp = renderer.badge(icon, dark = true, sizePx = 64)
            assertEquals(64, bmp.width)
        }
    }

    // AutofillId and the response's dataset list aren't public API; reach them for the test only.
    private fun autofillId(value: Int): AutofillId =
        AutofillId::class.java.getDeclaredConstructor(Int::class.javaPrimitiveType).apply { isAccessible = true }.newInstance(value)

    @Suppress("UNCHECKED_CAST")
    private fun datasetsOf(response: FillResponse): List<Dataset> =
        FillResponse::class.java.getDeclaredMethod("getDatasets").apply { isAccessible = true }.invoke(response) as List<Dataset>

    private fun datasetIdOf(dataset: Dataset): String? =
        Dataset::class.java.getDeclaredMethod("getId").apply { isAccessible = true }.invoke(dataset) as String?
}
