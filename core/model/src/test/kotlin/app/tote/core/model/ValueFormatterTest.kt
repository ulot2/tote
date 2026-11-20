package app.tote.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ValueFormatterTest {

    @Test fun displayDropsScheme() {
        assertEquals("www.example.site", ValueFormatter.display(EntryType.LINK, "https://www.example.site/"))
        assertEquals("github.com/me", ValueFormatter.display(EntryType.LINK, "github.com/me"))
    }

    @Test fun fillAddsSchemeOnlyForUrlFields() {
        assertEquals("https://github.com/me", ValueFormatter.forFill(EntryType.LINK, "github.com/me", targetIsUrlField = true))
        assertEquals("github.com/me", ValueFormatter.forFill(EntryType.LINK, "github.com/me", targetIsUrlField = false))
        assertEquals("http://a.com", ValueFormatter.forFill(EntryType.LINK, "http://a.com", targetIsUrlField = true))
    }

    @Test fun phoneIsNeverReformatted() =
        assertEquals("+1 (555) 010-0000", ValueFormatter.forFill(EntryType.PHONE, "+1 (555) 010-0000", targetIsUrlField = true))

    @Test fun longTextDisplayCollapsesWhitespace() =
        assertEquals("a b c", ValueFormatter.display(EntryType.LONG_TEXT, "a\n\nb   c"))
}
