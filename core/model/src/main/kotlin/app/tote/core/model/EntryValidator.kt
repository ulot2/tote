package app.tote.core.model

/** Editor validation. Errors block saving; warnings are shown but allow saving. */
object EntryValidator {

    sealed interface Issue {
        data object LabelBlank : Issue
        data object ValueBlank : Issue
        data object LabelTooLong : Issue
        data object EmailLooksWrong : Issue
        data object LinkLooksWrong : Issue
        data object PhoneLooksWrong : Issue
    }

    const val MAX_LABEL_LENGTH = 40

    data class Result(val errors: List<Issue>, val warnings: List<Issue>) {
        val canSave: Boolean get() = errors.isEmpty()
    }

    fun validate(label: String, value: String, type: EntryType): Result {
        val errors = buildList {
            if (label.isBlank()) add(Issue.LabelBlank)
            if (label.trim().length > MAX_LABEL_LENGTH) add(Issue.LabelTooLong)
            if (value.isBlank()) add(Issue.ValueBlank)
        }
        val warnings = buildList {
            if (value.isNotBlank()) {
                when (type) {
                    EntryType.EMAIL -> if (EntryClassifier.inferType(value) != EntryType.EMAIL) add(Issue.EmailLooksWrong)
                    EntryType.LINK -> if (!EntryClassifier.looksLikeUrl(value)) add(Issue.LinkLooksWrong)
                    EntryType.PHONE -> if (value.count(Char::isDigit) < 5) add(Issue.PhoneLooksWrong)
                    else -> Unit
                }
            }
        }
        return Result(errors, warnings)
    }
}
