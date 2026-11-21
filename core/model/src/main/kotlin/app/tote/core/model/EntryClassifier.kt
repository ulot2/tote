package app.tote.core.model

/**
 * Guesses an entry's [EntryType], [Category] and [EntryIcon] from what the user typed.
 * Used by the editor to auto-suggest; the user can always override.
 */
object EntryClassifier {

    data class Suggestion(val type: EntryType, val category: Category, val icon: EntryIcon)

    private val emailRegex = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")
    private val phoneRegex = Regex("^\\+?[\\d\\s().\\-]{7,}$")
    private val schemeRegex = Regex("^[a-zA-Z][a-zA-Z0-9+.\\-]*://")
    // host.tld with optional path, no spaces. Requires a dot and a 2+ letter TLD.
    private val bareDomainRegex = Regex("^(www\\.)?[a-zA-Z0-9\\-]+(\\.[a-zA-Z0-9\\-]+)*\\.[a-zA-Z]{2,}(:\\d+)?([/?#].*)?$")

    private val documentHosts = listOf("drive.google.com", "docs.google.com", "dropbox.com", "onedrive.live.com", "1drv.ms")
    private val documentLabelWords = listOf("cv", "resume", "résumé", "curriculum", "cover letter", "document", "doc")

    fun suggest(value: String, label: String = ""): Suggestion {
        val v = value.trim()
        val type = inferType(v, label)
        val icon = inferIcon(v, label, type)
        val category = inferCategory(v, label, type, icon)
        return Suggestion(type, category, icon)
    }

    fun inferType(value: String, label: String = ""): EntryType {
        val v = value.trim()
        if (v.isEmpty()) return typeFromLabel(label) ?: EntryType.SHORT_TEXT
        return when {
            emailRegex.matches(v) -> EntryType.EMAIL
            looksLikeUrl(v) -> EntryType.LINK
            phoneRegex.matches(v) && v.count(Char::isDigit) >= 7 -> EntryType.PHONE
            v.length > 80 || v.contains('\n') -> EntryType.LONG_TEXT
            else -> EntryType.SHORT_TEXT
        }
    }

    fun looksLikeUrl(value: String): Boolean {
        val v = value.trim()
        if (v.isEmpty() || v.any { it.isWhitespace() }) return false
        if (v.contains('@') && !schemeRegex.containsMatchIn(v)) return false
        return schemeRegex.containsMatchIn(v) || bareDomainRegex.matches(v)
    }

    fun inferIcon(value: String, label: String, type: EntryType): EntryIcon {
        if (type == EntryType.LINK) {
            brandForHost(hostOf(value))?.let { return it }
            if (isDocumentLink(value)) return EntryIcon.DOC
        }
        brandForLabel(label)?.let { brand ->
            // A label like "GitHub" only implies the brand icon for links or empty values.
            if (type == EntryType.LINK || value.isBlank()) return brand
        }
        val l = label.lowercase()
        if (type != EntryType.LINK && (l.contains("location") || l.contains("city") || l.contains("address"))) {
            return EntryIcon.LOCATION
        }
        if (type == EntryType.LINK && documentLabelWords.any { l.containsWord(it) }) return EntryIcon.DOC
        return EntryIcon.defaultFor(type)
    }

    private fun inferCategory(value: String, label: String, type: EntryType, icon: EntryIcon): Category {
        val l = label.lowercase()
        return when (type) {
            EntryType.EMAIL, EntryType.PHONE -> Category.CONTACT
            EntryType.LINK -> when {
                icon == EntryIcon.GOOGLE_DRIVE || icon == EntryIcon.DOC -> Category.DOCUMENTS
                isDocumentLink(value) || documentLabelWords.any { l.containsWord(it) } -> Category.DOCUMENTS
                else -> Category.LINKS
            }
            EntryType.SHORT_TEXT, EntryType.LONG_TEXT ->
                if (icon == EntryIcon.LOCATION) Category.CONTACT else Category.ANSWERS
        }
    }

    private fun typeFromLabel(label: String): EntryType? {
        val l = label.lowercase()
        return when {
            brandForLabel(label) != null -> EntryType.LINK
            l.containsWord("email") || l.containsWord("e-mail") -> EntryType.EMAIL
            l.containsWord("phone") || l.containsWord("mobile") -> EntryType.PHONE
            l.containsWord("portfolio") || l.containsWord("website") -> EntryType.LINK
            else -> null
        }
    }

    private fun isDocumentLink(value: String): Boolean {
        val host = hostOf(value)
        val lower = value.lowercase()
        return documentHosts.any { host == it || host.endsWith(".$it") } ||
            lower.substringBefore('?').endsWith(".pdf")
    }

    fun brandForHost(host: String): EntryIcon? {
        val h = host.lowercase().removePrefix("www.").removePrefix("m.")
        fun isHost(d: String) = h == d || h.endsWith(".$d")
        return when {
            isHost("linkedin.com") || isHost("lnkd.in") -> EntryIcon.LINKEDIN
            isHost("github.com") || isHost("github.io") -> EntryIcon.GITHUB
            isHost("x.com") || isHost("twitter.com") -> EntryIcon.X
            isHost("behance.net") -> EntryIcon.BEHANCE
            isHost("dribbble.com") -> EntryIcon.DRIBBBLE
            isHost("medium.com") -> EntryIcon.MEDIUM
            isHost("drive.google.com") || isHost("docs.google.com") -> EntryIcon.GOOGLE_DRIVE
            else -> null
        }
    }

    private fun brandForLabel(label: String): EntryIcon? {
        val l = label.lowercase()
        return when {
            l.contains("linkedin") -> EntryIcon.LINKEDIN
            l.contains("github") -> EntryIcon.GITHUB
            l.contains("twitter") || l.trim() == "x" -> EntryIcon.X
            l.contains("behance") -> EntryIcon.BEHANCE
            l.contains("dribbble") -> EntryIcon.DRIBBBLE
            l.contains("medium") -> EntryIcon.MEDIUM
            else -> null
        }
    }

    /** Host part of a URL-ish string, without scheme, credentials, port or path. Lowercase. */
    fun hostOf(value: String): String {
        val noScheme = value.trim().replaceFirst(schemeRegex, "")
        return noScheme.substringBefore('/').substringBefore('?').substringBefore('#')
            .substringAfterLast('@').substringBefore(':').lowercase()
    }

    private fun String.containsWord(word: String): Boolean =
        Regex("(^|[^\\p{L}])${Regex.escape(word)}($|[^\\p{L}])").containsMatchIn(this)
}
