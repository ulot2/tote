package app.tote.core.matching

import app.tote.core.model.EntryType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Runs every fixture in `fixtures/job_forms.json` (Plan §7.6) and reports all failures at once,
 * so tuning the dictionary shows the whole picture rather than one case at a time.
 */
class FixtureTest {

    @Serializable
    data class FieldJson(
        val autofillHints: List<String> = emptyList(),
        val htmlAutocomplete: String? = null,
        val htmlType: String? = null,
        val htmlName: String? = null,
        val htmlId: String? = null,
        val placeholder: String? = null,
        val ariaLabel: String? = null,
        val label: String? = null,
        val hintText: String? = null,
        val resourceId: String? = null,
        val contentDescription: String? = null,
        val nearbyText: String? = null,
        val inputKind: InputKind = InputKind.TEXT,
        val importantForAutofill: Boolean = true,
        val webDomain: String? = null,
        val packageName: String? = null,
    ) {
        fun toSignals() = FieldSignals(
            autofillHints, htmlAutocomplete, htmlType, htmlName, htmlId, placeholder, ariaLabel, label, hintText,
            resourceId, contentDescription, nearbyText, inputKind, importantForAutofill = importantForAutofill,
            webDomain = webDomain, packageName = packageName,
        )
    }

    @Serializable
    data class Expect(
        val top: String? = null,
        val contains: List<String> = emptyList(),
        val absent: List<String> = emptyList(),
        val only: List<String>? = null,
        val ignored: IgnoreReason? = null,
        val notIgnored: Boolean = false,
        val none: Boolean = false,
        val fallback: Boolean = false,
        val allLinks: Boolean = false,
    )

    @Serializable data class Context(val activeProfileId: Long? = null)

    @Serializable
    data class Fixture(val id: String, val platform: String, val field: FieldJson, val expect: Expect, val context: Context = Context())

    @Serializable data class File(val fixtures: List<Fixture>)

    private val fixtures: List<Fixture> = Json { ignoreUnknownKeys = true }.decodeFromString<File>(
        javaClass.getResourceAsStream("/fixtures/job_forms.json")!!.bufferedReader().readText(),
    ).fixtures

    private val matcher = FieldMatcher()

    @Test fun atLeastFortyFixtures() = assertTrue("Plan §7.6 asks for 40+ fixtures, have ${fixtures.size}", fixtures.size >= 40)

    @Test fun allFixturesBehaveAsSpecified() {
        val failures = fixtures.mapNotNull { f -> check(f)?.let { "${f.id} (${f.platform}): $it" } }
        assertTrue("${failures.size}/${fixtures.size} fixtures failed:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    private fun check(f: Fixture): String? {
        val result = matcher.match(f.field.toSignals(), TestVault.all, MatchContext(activeProfileId = f.context.activeProfileId))
        val labels = result.suggestions.map { it.entry.label }
        val shown = "got ${if (result.ignored != null) "ignored=${result.ignored}" else labels.toString()}" +
            (if (result.isFallback) " (fallback)" else "") +
            " | " + result.suggestions.joinToString { "${it.entry.label}=${it.score} ${it.reasons}" }
        val e = f.expect
        return when {
            e.ignored != null && result.ignored != e.ignored -> "expected ignored=${e.ignored}, $shown"
            e.ignored != null -> null
            result.ignored != null -> "expected suggestions, $shown"
            e.none && (labels.isNotEmpty() || result.showAllEntries) -> "expected nothing at all, $shown"
            e.top != null && labels.firstOrNull() != e.top -> "expected top '${e.top}', $shown"
            e.top != null && result.isFallback -> "expected a real match for '${e.top}', $shown"
            !e.contains.all { it in labels } -> "expected to contain ${e.contains}, $shown"
            e.absent.any { it in labels } -> "expected none of ${e.absent}, $shown"
            e.only != null && labels != e.only -> "expected exactly ${e.only}, $shown"
            e.fallback && !result.isFallback -> "expected fallback, $shown"
            e.allLinks && (labels.isEmpty() || result.suggestions.any { it.entry.type != EntryType.LINK }) -> "expected only links, $shown"
            !e.none && !result.showAllEntries -> "expected the All entries option, $shown"
            else -> null
        }
    }
}
