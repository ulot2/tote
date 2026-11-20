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

    @Test fun keepsAccentedLetters() = assertEquals("résumé cv", TextNormalizer.normalize("Résumé/CV"))

    @Test fun tokens() = assertEquals(listOf("e", "mail", "address"), TextNormalizer.tokens("E-mail Address"))
}
