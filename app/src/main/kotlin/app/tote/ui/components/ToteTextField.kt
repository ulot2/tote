package app.tote.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import app.tote.ui.theme.Tote
import app.tote.ui.theme.ToteShapes

enum class FieldMessageKind { Error, Warning, Hint }

data class FieldMessage(val text: String, val kind: FieldMessageKind)

/**
 * Tote's text field: label above, soft filled container, accent ring on focus.
 * Built on BasicTextField so it can match the design system exactly.
 */
@Composable
fun ToteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    textStyle: TextStyle = Tote.type.body,
    singleLine: Boolean = true,
    minLines: Int = 1,
    message: FieldMessage? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = Tote.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val isError = message?.kind == FieldMessageKind.Error
    val ring by animateColorAsState(
        when {
            isError -> c.danger
            focused -> c.accentContent
            else -> Color.Transparent
        },
        label = "fieldRing",
    )

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = Tote.type.label, color = c.textSecondary, modifier = Modifier.padding(start = 4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = minLines,
            textStyle = textStyle.copy(color = c.textPrimary),
            cursorBrush = SolidColor(c.accentContent),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interaction,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = label
                    if (isError) error(message!!.text)
                },
            decorationBox = { inner ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                        .background(c.surface, ToteShapes.field)
                        .border(1.5.dp, ring, ToteShapes.field)
                        .border(1.dp, if (ring == Color.Transparent) c.outline else Color.Transparent, ToteShapes.field)
                        .padding(start = 16.dp, end = if (trailing != null) 6.dp else 16.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(vertical = 10.dp),
                    ) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(placeholder, style = textStyle, color = c.textTertiary, maxLines = if (singleLine) 1 else Int.MAX_VALUE)
                        }
                        inner()
                    }
                    trailing?.invoke()
                }
            },
        )
        AnimatedVisibility(visible = message != null) {
            val m = message ?: return@AnimatedVisibility
            Text(
                m.text,
                style = Tote.type.bodySmall,
                color = when (m.kind) {
                    FieldMessageKind.Error -> c.danger
                    FieldMessageKind.Warning -> c.warning
                    FieldMessageKind.Hint -> c.textSecondary
                },
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}
