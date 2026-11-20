package app.tote.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryValidatorTest {

    @Test fun blankFieldsBlockSave() {
        val r = EntryValidator.validate(" ", "", EntryType.SHORT_TEXT)
        assertFalse(r.canSave)
        assertEquals(listOf(EntryValidator.Issue.LabelBlank, EntryValidator.Issue.ValueBlank), r.errors)
    }

    @Test fun badEmailIsWarningNotError() {
        val r = EntryValidator.validate("Email", "not-an-email", EntryType.EMAIL)
        assertTrue(r.canSave)
        assertEquals(listOf(EntryValidator.Issue.EmailLooksWrong), r.warnings)
    }

    @Test fun goodLinkHasNoIssues() {
        val r = EntryValidator.validate("GitHub", "github.com/me", EntryType.LINK)
        assertTrue(r.errors.isEmpty() && r.warnings.isEmpty())
    }

    @Test fun longLabelBlocksSave() =
        assertFalse(EntryValidator.validate("x".repeat(41), "v", EntryType.SHORT_TEXT).canSave)
}
