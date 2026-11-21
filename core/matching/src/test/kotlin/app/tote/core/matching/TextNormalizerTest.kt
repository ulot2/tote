package app.tote.core.matching

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {

    @Test fun splitsCamelCase() = assertEquals("linkedin profile url", TextNormalizer.normalize("linkedinProfileURL"))

    @Test fun splitsSnakeAndKebab() = assertEquals("job application github url", TextNormalizer.normalize("job_application[github-url]"))

    @Test fun stripsPunctuationAndCollapsesWhitespace() =
        assertEquals("your linkedin profile", TextNormalizer.normalize("  Your   Linkedin profile:* "))

    /** Brand camel-case splits; the Phase 3 dictionary lists "linked in" / "git hub" as synonyms. */
    @Test fun brandCamelCaseSplits() = assertEquals("linked in", TextNormalizer.normalize("LinkedIn"))

    @Test fun foldsAccents() = assertEquals("resume cv", TextNormalizer.normalize("Résumé/CV"))

    @Test fun stemsPluralsButNotAddress() {
        assertEquals("link", TextNormalizer.stem("links"))
        assertEquals("address", TextNormalizer.stem("address"))
        assertEquals("cv", TextNormalizer.stem("cv"))
    }

    @Test fun tokens() = assertEquals(listOf("e", "mail", "address"), TextNormalizer.tokens("E-mail Address"))
}
