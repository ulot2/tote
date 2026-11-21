package app.tote.core.matching

import app.tote.core.model.Category
import app.tote.core.model.Entry
import app.tote.core.model.EntryIcon
import app.tote.core.model.EntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldMatcherTest {

    private val matcher = FieldMatcher()
    private fun match(field: FieldSignals, ctx: MatchContext = MatchContext(), entries: List<Entry> = TestVault.all) =
        matcher.match(field, entries, ctx)

    @Test fun returnsAtMostThreeSuggestions() {
        val r = match(FieldSignals(label = "Other link"))
        assertEquals(3, r.suggestions.size)
        assertTrue(r.showAllEntries)
    }

    @Test fun learnedMappingBeatsDictionary() {
        val field = FieldSignals(label = "Website", webDomain = "jobs.example.com")
        assertEquals("Portfolio", match(field).top?.label)
        val learned = match(field, MatchContext(learned = mapOf(TestVault.github.id to 1)))
        assertEquals("GitHub", learned.top?.label)
    }

    @Test fun activeProfileFiltersBeforeScoring() {
        val field = FieldSignals(label = "Resume")
        assertEquals("CV (Tech)", match(field, MatchContext(activeProfileId = 1)).top?.label)
        assertEquals("CV (Non-tech)", match(field, MatchContext(activeProfileId = 2)).top?.label)
        // Entries in "All" profiles stay eligible in every profile.
        assertEquals("LinkedIn", match(FieldSignals(label = "LinkedIn"), MatchContext(activeProfileId = 2)).top?.label)
    }

    @Test fun pinnedAndUsageOnlyBreakTies() {
        // LinkedIn is pinned and most used, but a GitHub field must still put GitHub first.
        assertEquals("GitHub", match(FieldSignals(label = "GitHub")).top?.label)
        // Among equal link scores, pinned + used wins.
        assertEquals("LinkedIn", match(FieldSignals(label = "Other link")).top?.label)
    }

    @Test fun emailFieldExcludesEverythingElse() {
        val r = match(FieldSignals(label = "Contact", htmlType = "email"))
        assertTrue(r.suggestions.all { it.entry.type == EntryType.EMAIL })
    }

    @Test fun urlFieldSinksNonLinks() {
        val r = match(FieldSignals(label = "Summary", htmlType = "url"))
        assertNotEquals("Short bio", r.top?.label)
    }

    @Test fun userKeywordsMatch() {
        val entries = TestVault.all + Entry(99, "Kaggle", "kaggle.com/me", EntryType.LINK, Category.LINKS, EntryIcon.GLOBE, keywords = listOf("data science profile"))
        assertEquals("Kaggle", match(FieldSignals(label = "Your data science profile"), entries = entries).top?.label)
    }

    @Test fun entryLabelMatchesEvenWithoutDictionaryKind() {
        val entries = TestVault.all + Entry(98, "Shoe size", "42", EntryType.SHORT_TEXT, Category.ANSWERS, EntryIcon.TEXT)
        assertEquals("Shoe size", match(FieldSignals(label = "Shoe size (EU)"), entries = entries).top?.label)
    }

    @Test fun longerPhraseSwallowsWeakerShorterOne() {
        val r = match(FieldSignals(label = "Email address"))
        assertEquals(listOf("Email"), r.suggestions.map { it.entry.label })
    }

    @Test fun multilineFallbackPrefersLongAnswers() {
        val r = match(FieldSignals(label = "Anything else?", htmlType = "textarea"))
        assertTrue(r.isFallback)
        assertEquals(EntryType.LONG_TEXT, r.top?.type)
    }

    @Test fun noFallbackOnNumberOrDateFields() {
        for (type in listOf("number", "date")) {
            val r = match(FieldSignals(label = "Something unrelated", htmlType = type))
            assertTrue(r.suggestions.isEmpty())
            assertFalse(r.showAllEntries)
        }
    }

    @Test fun emptyVaultStillOffersAllEntriesOnTextFields() {
        val r = match(FieldSignals(label = "LinkedIn"), entries = emptyList())
        assertTrue(r.suggestions.isEmpty())
        assertTrue(r.showAllEntries)
    }

    @Test fun reasonsNeverContainValues() {
        val r = match(FieldSignals(label = "LinkedIn URL", htmlName = "linkedin"))
        val reasons = r.suggestions.flatMap { it.reasons }.joinToString(" ")
        TestVault.all.forEach { assertFalse(reasons.contains(it.value)) }
    }

    @Test fun fastEnoughForAutofill() {
        // Plan §10: respond in under ~100 ms. 200 entries × 30 fields here should take a few ms each.
        val big = (1..200).map { i ->
            Entry(1000L + i, "Answer $i", "value $i", EntryType.SHORT_TEXT, Category.ANSWERS, EntryIcon.TEXT, keywords = listOf("keyword $i"))
        } + TestVault.all
        val fields = (1..30).map { FieldSignals(label = "LinkedIn profile $it", htmlName = "field_$it", nearbyText = "Section $it") }
        repeat(3) { fields.forEach { f -> matcher.match(f, big) } } // warm up the JIT
        val start = System.nanoTime()
        fields.forEach { f -> matcher.match(f, big) }
        val perFieldMs = (System.nanoTime() - start) / 1_000_000.0 / fields.size
        assertTrue("took $perFieldMs ms per field", perFieldMs < 50)
    }
}
