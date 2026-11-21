package app.tote.core.matching

/** Fields Tote must never offer suggestions in (Plan §7.5). */
object IgnoreRules {

    fun check(field: FieldSignals, dictionary: KeywordDictionary = KeywordDictionary.default): IgnoreReason? {
        if (!field.importantForAutofill) return IgnoreReason.NOT_IMPORTANT
        when (field.effectiveKind) {
            InputKind.PASSWORD -> return IgnoreReason.PASSWORD
            InputKind.HIDDEN -> return IgnoreReason.HIDDEN
            else -> Unit
        }
        if (field.htmlType.equals("search", ignoreCase = true)) return IgnoreReason.SEARCH

        // Hints and autocomplete are explicit, so they win over words.
        for (hint in field.autofillHints) {
            (dictionary.androidHint(hint) as? HintMeaning.Ignore)?.let { return it.reason }
        }
        field.htmlAutocomplete?.let { ac -> (dictionary.autocomplete(ac) as? HintMeaning.Ignore)?.let { return it.reason } }

        val describing = listOfNotNull(
            field.label, field.placeholder, field.ariaLabel, field.hintText, field.contentDescription,
            field.htmlName, field.htmlId, field.resourceId,
        ).map(::SignalText)
        val ids = listOfNotNull(field.htmlName, field.htmlId, field.resourceId).map(::SignalText)
        val visible = listOfNotNull(field.label, field.placeholder, field.ariaLabel, field.hintText).map(::SignalText)

        for (reason in listOf(IgnoreReason.PASSWORD, IgnoreReason.OTP, IgnoreReason.CARD, IgnoreReason.CAPTCHA)) {
            val phrases = dictionary.ignore[reason].orEmpty()
            if (describing.any { t -> phrases.any { p -> PhraseMatcher.find(t, p) != null } }) return reason
        }

        // Search is matched narrowly: an id like "q"/"search", or a label that *starts* with "search".
        // "Job search status" stays a normal field.
        val search = dictionary.ignore[IgnoreReason.SEARCH].orEmpty()
        val idIsSearch = ids.any { t -> search.any { p -> t.tokens == p.tokens } || t.tokens.firstOrNull() == "search" }
        val labelStartsWithSearch = visible.any { t -> t.tokens.firstOrNull() == "search" }
        if (idIsSearch || labelStartsWithSearch) return IgnoreReason.SEARCH

        return null
    }
}
