package app.tote.feature.autofill

import android.text.InputType
import android.view.View
import app.tote.core.matching.FieldSignals
import app.tote.core.matching.InputKind

/**
 * The parts of an `AssistStructure.ViewNode` the parser reads. An interface so the parsing rules
 * can be tested on the JVM with hand-built trees; [AssistNodeView] adapts the real thing.
 */
interface NodeView<ID> {
    val autofillId: ID?
    val autofillType: Int
    val className: String?
    val visibility: Int
    val isEnabled: Boolean
    val isFocused: Boolean
    val autofillHints: List<String>
    val htmlTag: String?
    /** HTML attributes with lowercase names, e.g. "type", "name", "aria-label". */
    val htmlAttributes: Map<String, String>
    val hint: String?
    val idEntry: String?
    val contentDescription: String?
    /** Visible text, read only from non-editable views (labels). Never a field's typed value. */
    val text: String?
    val inputType: Int
    val importantForAutofill: Int
    val webDomain: String?
    val children: List<NodeView<ID>>
}

data class ParsedField<ID>(val id: ID, val signals: FieldSignals, val focused: Boolean)

/**
 * Walks a screen's view structure and collects every fillable text field with its signals
 * (Plan §9.4 step 2). Nothing here keeps a reference to the structure after returning.
 */
object StructureParser {

    private const val MAX_LABEL_LENGTH = 120
    private val nonTextInputTypes = setOf("checkbox", "radio", "submit", "button", "file", "range", "color", "image", "reset")

    fun <ID> parse(windows: List<NodeView<ID>>, packageName: String?): List<ParsedField<ID>> {
        val out = mutableListOf<ParsedField<ID>>()
        for (root in windows) {
            val walker = Walker(out, packageName)
            walker.visit(root, inheritedDomain = null, excluded = false)
        }
        return out
    }

    private class Walker<ID>(val out: MutableList<ParsedField<ID>>, val packageName: String?) {
        /** The last visible text seen in document order: the field's "nearby label" (Plan §7.1 item 4). */
        var lastText: String? = null

        fun visit(node: NodeView<ID>, inheritedDomain: String?, excluded: Boolean) {
            if (node.visibility != View.VISIBLE) return
            val domain = node.webDomain?.takeIf { it.isNotBlank() } ?: inheritedDomain
            val importance = node.importantForAutofill
            val selfExcluded = excluded || importance == View.IMPORTANT_FOR_AUTOFILL_NO ||
                importance == View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            val childrenExcluded = excluded || importance == View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS ||
                importance == View.IMPORTANT_FOR_AUTOFILL_YES_EXCLUDE_DESCENDANTS

            val id = node.autofillId
            if (id != null && isFillable(node)) {
                out += ParsedField(id, signalsOf(node, domain, selfExcluded), node.isFocused)
            } else {
                node.text?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_LABEL_LENGTH }?.let { lastText = it }
            }
            node.children.forEach { visit(it, domain, childrenExcluded) }
        }

        private fun isFillable(node: NodeView<ID>): Boolean {
            if (node.autofillType != View.AUTOFILL_TYPE_TEXT || !node.isEnabled) return false
            val tag = node.htmlTag?.lowercase()
            if (tag != null) {
                if (tag == "textarea") return true
                if (tag != "input") return false
                val type = node.htmlAttributes["type"]?.lowercase()
                return type == null || type !in nonTextInputTypes
            }
            return node.className?.contains("EditText") == true || node.inputType != InputType.TYPE_NULL
        }

        private fun signalsOf(node: NodeView<ID>, domain: String?, excluded: Boolean): FieldSignals {
            val attrs = node.htmlAttributes
            val tag = node.htmlTag?.lowercase()
            val inputType = node.inputType
            return FieldSignals(
                autofillHints = node.autofillHints,
                htmlAutocomplete = attrs["autocomplete"],
                htmlType = if (tag == "textarea") "textarea" else attrs["type"],
                htmlName = attrs["name"],
                htmlId = attrs["id"],
                placeholder = attrs["placeholder"],
                ariaLabel = attrs["aria-label"],
                label = attrs["label"],
                hintText = node.hint,
                resourceId = node.idEntry,
                contentDescription = node.contentDescription,
                nearbyText = lastText,
                inputKind = kindOf(inputType),
                multiline = (inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT &&
                    (inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0,
                importantForAutofill = !excluded,
                webDomain = domain,
                packageName = packageName,
            )
        }
    }

    /** Maps Android input type flags to the matcher's coarse kind. */
    fun kindOf(inputType: Int): InputKind {
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return when (inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_TEXT -> when (variation) {
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> InputKind.EMAIL
                InputType.TYPE_TEXT_VARIATION_PASSWORD, InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> InputKind.PASSWORD
                InputType.TYPE_TEXT_VARIATION_URI -> InputKind.URI
                else -> InputKind.TEXT
            }
            InputType.TYPE_CLASS_NUMBER ->
                if (variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD) InputKind.PASSWORD else InputKind.NUMBER
            InputType.TYPE_CLASS_PHONE -> InputKind.PHONE
            InputType.TYPE_CLASS_DATETIME -> InputKind.DATE
            else -> InputKind.TEXT
        }
    }
}
