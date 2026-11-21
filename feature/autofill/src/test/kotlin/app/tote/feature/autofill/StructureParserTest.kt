package app.tote.feature.autofill

import android.text.InputType
import android.view.View
import app.tote.core.matching.InputKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StructureParserTest {

    private data class Node(
        override val autofillId: String? = null,
        override val autofillType: Int = View.AUTOFILL_TYPE_NONE,
        override val className: String? = null,
        override val visibility: Int = View.VISIBLE,
        override val isEnabled: Boolean = true,
        override val isFocused: Boolean = false,
        override val autofillHints: List<String> = emptyList(),
        override val htmlTag: String? = null,
        override val htmlAttributes: Map<String, String> = emptyMap(),
        override val hint: String? = null,
        override val idEntry: String? = null,
        override val contentDescription: String? = null,
        override val text: String? = null,
        override val inputType: Int = 0,
        override val importantForAutofill: Int = View.IMPORTANT_FOR_AUTOFILL_AUTO,
        override val webDomain: String? = null,
        override val children: List<NodeView<String>> = emptyList(),
    ) : NodeView<String>

    private fun editText(id: String, hint: String? = null, inputType: Int = InputType.TYPE_CLASS_TEXT, focused: Boolean = false, idEntry: String? = null) =
        Node(id, View.AUTOFILL_TYPE_TEXT, "android.widget.EditText", hint = hint, inputType = inputType, isFocused = focused, idEntry = idEntry)

    private fun label(text: String) = Node(className = "android.widget.TextView", text = text)

    private fun input(id: String, vararg attrs: Pair<String, String>, tag: String = "input") =
        Node(id, View.AUTOFILL_TYPE_TEXT, htmlTag = tag, htmlAttributes = attrs.toMap())

    @Test fun nativeFieldsWithLabelsAndFlags() {
        val root = Node(
            children = listOf(
                label("LinkedIn profile"),
                editText("a", idEntry = "et_linkedin", focused = true),
                label("Email"),
                editText("b", inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS),
                editText("c", hint = "About you", inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE),
            ),
        )
        val fields = StructureParser.parse(listOf(root), "com.example")
        assertEquals(listOf("a", "b", "c"), fields.map { it.id })
        assertEquals("LinkedIn profile", fields[0].signals.nearbyText)
        assertEquals("et_linkedin", fields[0].signals.resourceId)
        assertTrue(fields[0].focused)
        assertEquals(InputKind.EMAIL, fields[1].signals.inputKind)
        assertTrue(fields[2].signals.multiline)
        assertEquals("com.example", fields[2].signals.packageName)
    }

    @Test fun webFieldsReadHtmlAttributesAndInheritDomain() {
        val web = Node(
            webDomain = "boards.greenhouse.io",
            children = listOf(
                input("li", "type" to "text", "name" to "job_application[linkedin]", "aria-label" to "LinkedIn Profile", "autocomplete" to "off"),
                input("why", "name" to "why", tag = "textarea"),
                input("cb", "type" to "checkbox", "name" to "consent"),
                input("go", "type" to "submit"),
            ),
        )
        val fields = StructureParser.parse(listOf(Node(children = listOf(web))), "com.android.chrome")
        assertEquals(listOf("li", "why"), fields.map { it.id })
        val li = fields[0].signals
        assertEquals("LinkedIn Profile", li.ariaLabel)
        assertEquals("job_application[linkedin]", li.htmlName)
        assertEquals("off", li.htmlAutocomplete)
        assertEquals("boards.greenhouse.io", li.webDomain)
        assertEquals("boards.greenhouse.io", li.source)
        assertEquals("textarea", fields[1].signals.htmlType)
    }

    @Test fun excludedSubtreesAndHiddenViews() {
        val root = Node(
            children = listOf(
                Node(importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS, children = listOf(editText("x"))),
                editText("gone").copy(visibility = View.GONE),
                editText("disabled").copy(isEnabled = false),
                editText("ok"),
            ),
        )
        val fields = StructureParser.parse(listOf(root), null)
        assertEquals(listOf("x", "ok"), fields.map { it.id })
        assertFalse("descendants of an excluded subtree are marked not important", fields[0].signals.importantForAutofill)
        assertTrue(fields[1].signals.importantForAutofill)
    }

    @Test fun labelsLongerThanALabelAreSkipped() {
        val root = Node(children = listOf(label("Portfolio"), label("x".repeat(500)), editText("a")))
        assertEquals("Portfolio", StructureParser.parse(listOf(root), null).single().signals.nearbyText)
    }

    @Test fun inputTypeMapping() {
        assertEquals(InputKind.PASSWORD, StructureParser.kindOf(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD))
        assertEquals(InputKind.PASSWORD, StructureParser.kindOf(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD))
        assertEquals(InputKind.URI, StructureParser.kindOf(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI))
        assertEquals(InputKind.PHONE, StructureParser.kindOf(InputType.TYPE_CLASS_PHONE))
        assertEquals(InputKind.NUMBER, StructureParser.kindOf(InputType.TYPE_CLASS_NUMBER))
        assertEquals(InputKind.DATE, StructureParser.kindOf(InputType.TYPE_CLASS_DATETIME))
        assertEquals(InputKind.TEXT, StructureParser.kindOf(InputType.TYPE_NULL))
    }

    @Test fun nothingFillable() {
        assertTrue(StructureParser.parse(listOf(Node(children = listOf(label("Hello")))), null).isEmpty())
        assertNull(StructureParser.parse(listOf(Node(children = listOf(label("A"), editText("a")))), null).single().signals.hintText)
    }
}
