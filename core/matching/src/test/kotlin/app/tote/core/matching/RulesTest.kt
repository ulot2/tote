package app.tote.core.matching

import app.tote.core.model.Category
import app.tote.core.model.Entry
import app.tote.core.model.EntryIcon
import app.tote.core.model.EntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesTest {

    @Test fun dictionaryPathSurvivesMinification() {
        // A relative path broke every fill request in the minified 0.2.0 build. Keep it absolute,
        // and make sure the file is where that path says, independent of this class's package.
        assertTrue(KeywordDictionary.RESOURCE_PATH.startsWith("/"))
        assertNotNull(ClassLoader.getSystemResource(KeywordDictionary.RESOURCE_PATH.removePrefix("/")))
    }

    @Test fun dictionaryLoadsEveryKind() {
        val d = KeywordDictionary.default
        EntryKind.entries.forEach { assertTrue("no phrases for $it", d.strong[it].orEmpty().isNotEmpty()) }
    }

    @Test fun autocompleteReadsTheMeaningfulToken() {
        val d = KeywordDictionary.default
        assertEquals(HintMeaning.Kind(EntryKind.PHONE), d.autocomplete("section-apply shipping tel"))
        assertEquals(HintMeaning.Link, d.autocomplete("url"))
        assertNull(d.autocomplete("off"))
    }

    @Test fun androidHintsAreCaseAndPunctuationInsensitive() {
        assertEquals(HintMeaning.Kind(EntryKind.EMAIL), KeywordDictionary.default.androidHint("emailAddress"))
        assertEquals(HintMeaning.Ignore(IgnoreReason.OTP), KeywordDictionary.default.androidHint("smsOTPCode"))
    }

    private fun entry(label: String, value: String, type: EntryType, icon: EntryIcon) =
        Entry(1, label, value, type, Category.LINKS, icon)

    @Test fun entryKinds() {
        assertEquals(EntryKind.LINKEDIN, EntryKinds.of(entry("Work profile", "linkedin.com/in/x", EntryType.LINK, EntryIcon.LINKEDIN)))
        assertEquals(EntryKind.EMAIL, EntryKinds.of(entry("Work", "a@b.co", EntryType.EMAIL, EntryIcon.MAIL)))
        assertEquals(EntryKind.CV, EntryKinds.of(entry("CV (Non-tech)", "drive.google.com/x", EntryType.LINK, EntryIcon.GOOGLE_DRIVE)))
        assertEquals(EntryKind.COVER_LETTER, EntryKinds.of(entry("Cover letter", "drive.google.com/y", EntryType.LINK, EntryIcon.DOC)))
        assertEquals(EntryKind.WHY_COMPANY, EntryKinds.of(entry("Why this company?", "...", EntryType.LONG_TEXT, EntryIcon.TEXT)))
        assertEquals(EntryKind.PORTFOLIO, EntryKinds.of(entry("My site", "me.dev", EntryType.LINK, EntryIcon.GLOBE)))
        assertEquals(EntryKind.YEARS_EXPERIENCE, EntryKinds.of(entry("Years of experience", "5", EntryType.SHORT_TEXT, EntryIcon.TEXT)))
        assertNull(EntryKinds.of(entry("Pronouns", "they/them", EntryType.SHORT_TEXT, EntryIcon.TEXT)))
    }

    @Test fun ignoreRulesAreNarrowForSearch() {
        assertEquals(IgnoreReason.SEARCH, IgnoreRules.check(FieldSignals(placeholder = "Search roles")))
        assertNull(IgnoreRules.check(FieldSignals(label = "Job search status")))
        assertNull(IgnoreRules.check(FieldSignals(label = "How did you find us?")))
    }

    @Test fun cvIsNotFoundInsideCvv() {
        assertEquals(IgnoreReason.CARD, IgnoreRules.check(FieldSignals(htmlName = "cvv")))
        val hit = PhraseMatcher.find(SignalText("cvvcode"), Phrase.of("cv"))
        assertNull(hit)
    }

    @Test fun signatureIsStableAndValueFree() {
        val a = FieldSignals(label = "LinkedIn Profile", htmlName = "urls[LinkedIn]", webDomain = "www.Jobs.Lever.co")
        val b = a.copy()
        assertEquals(FieldSignature.key(a), FieldSignature.key(b))
        assertEquals("jobs.lever.co", FieldSignature.key(a)!!.source)
        assertNotEquals(FieldSignature.of(a), FieldSignature.of(a.copy(htmlName = "urls[GitHub]")))
        assertNull(FieldSignature.key(FieldSignals(label = "x")))
    }
}
