package app.tote.feature.autofill

import android.app.assist.AssistStructure
import android.view.autofill.AutofillId

/** Adapts a real [AssistStructure.ViewNode] to [NodeView]. Lazily reads children. */
class AssistNodeView(private val node: AssistStructure.ViewNode) : NodeView<AutofillId> {
    override val autofillId: AutofillId? get() = node.autofillId
    override val autofillType: Int get() = node.autofillType
    override val className: String? get() = node.className
    override val visibility: Int get() = node.visibility
    override val isEnabled: Boolean get() = node.isEnabled
    override val isFocused: Boolean get() = node.isFocused
    override val autofillHints: List<String> get() = node.autofillHints?.toList().orEmpty()
    override val htmlTag: String? get() = node.htmlInfo?.tag
    override val htmlAttributes: Map<String, String>
        get() = node.htmlInfo?.attributes.orEmpty()
            .mapNotNull { p -> p.first?.let { k -> p.second?.let { v -> k.lowercase() to v } } }
            .toMap()
    override val hint: String? get() = node.hint
    override val idEntry: String? get() = node.idEntry
    override val contentDescription: String? get() = node.contentDescription?.toString()

    // Only non-editable views contribute text, and only as labels: a field's own value is never read.
    override val text: String? get() = if (node.autofillType == android.view.View.AUTOFILL_TYPE_NONE) node.text?.toString() else null
    override val inputType: Int get() = node.inputType
    override val importantForAutofill: Int get() = node.importantForAutofill
    override val webDomain: String? get() = node.webDomain
    override val children: List<NodeView<AutofillId>>
        get() = (0 until node.childCount).map { AssistNodeView(node.getChildAt(it)) }

    companion object {
        fun windows(structure: AssistStructure): List<NodeView<AutofillId>> =
            (0 until structure.windowNodeCount).map { AssistNodeView(structure.getWindowNodeAt(it).rootViewNode) }
    }
}
