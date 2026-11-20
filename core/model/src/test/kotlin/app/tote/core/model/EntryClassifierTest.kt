package app.tote.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryClassifierTest {

    private fun assertSuggests(value: String, label: String, type: EntryType, category: Category, icon: EntryIcon) {
        assertEquals(EntryClassifier.Suggestion(type, category, icon), EntryClassifier.suggest(value, label))
    }

    @Test fun linkedinUrlWithoutScheme() =
        assertSuggests("linkedin.com/in/someone", "", EntryType.LINK, Category.LINKS, EntryIcon.LINKEDIN)

    @Test fun githubUrlWithScheme() =
        assertSuggests("https://github.com/someone", "Code", EntryType.LINK, Category.LINKS, EntryIcon.GITHUB)

    @Test fun twitterAndX() {
        assertEquals(EntryIcon.X, EntryClassifier.suggest("https://twitter.com/me").icon)
        assertEquals(EntryIcon.X, EntryClassifier.suggest("x.com/me").icon)
    }

    @Test fun portfolioIsGlobe() =
        assertSuggests("https://www.example.site", "Portfolio", EntryType.LINK, Category.LINKS, EntryIcon.GLOBE)

    @Test fun googleDriveIsDocument() =
        assertSuggests("https://drive.google.com/file/d/abc/view", "CV", EntryType.LINK, Category.DOCUMENTS, EntryIcon.GOOGLE_DRIVE)

    @Test fun pdfLinkIsDocument() =
        assertSuggests("example.com/files/resume.pdf", "", EntryType.LINK, Category.DOCUMENTS, EntryIcon.DOC)

    @Test fun cvLabelOnGenericLinkIsDocument() =
        assertSuggests("example.com/cv", "CV (Non-tech)", EntryType.LINK, Category.DOCUMENTS, EntryIcon.DOC)

    @Test fun email() =
        assertSuggests("someone@example.com", "", EntryType.EMAIL, Category.CONTACT, EntryIcon.MAIL)

    @Test fun phone() =
        assertSuggests("+234 801 234 5678", "", EntryType.PHONE, Category.CONTACT, EntryIcon.PHONE)

    @Test fun shortNumberIsNotPhone() =
        assertEquals(EntryType.SHORT_TEXT, EntryClassifier.inferType("5"))

    @Test fun location() =
        assertSuggests("Lagos, Nigeria", "Location", EntryType.SHORT_TEXT, Category.CONTACT, EntryIcon.LOCATION)

    @Test fun longAnswer() {
        val bio = "I am a developer who enjoys building calm, useful tools. I have shipped apps used by thousands of people."
        assertSuggests(bio, "Short bio", EntryType.LONG_TEXT, Category.ANSWERS, EntryIcon.TEXT)
    }

    @Test fun emptyValueUsesLabel() {
        assertSuggests("", "LinkedIn", EntryType.LINK, Category.LINKS, EntryIcon.LINKEDIN)
        assertEquals(EntryType.EMAIL, EntryClassifier.inferType("", "Work email"))
    }

    @Test fun sentenceIsNotUrl() {
        assertFalse(EntryClassifier.looksLikeUrl("hello world.com"))
        assertFalse(EntryClassifier.looksLikeUrl("someone@example.com"))
        assertTrue(EntryClassifier.looksLikeUrl("tnuell.site"))
    }

    @Test fun lookalikeHostIsNotBrand() {
        assertEquals(null, EntryClassifier.brandForHost("notlinkedin.com"))
        assertEquals(EntryIcon.LINKEDIN, EntryClassifier.brandForHost("ng.linkedin.com"))
    }

    @Test fun hostOfStripsEverything() =
        assertEquals("github.com", EntryClassifier.hostOf("https://user@GitHub.com:443/path?q=1"))
}
