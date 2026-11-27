package app.tote.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tote.core.model.EntryType
import app.tote.core.model.SeedTemplate
import app.tote.feature.vault.TuckWordmark
import app.tote.ui.components.PillButton
import app.tote.ui.components.PillStyle
import app.tote.ui.components.ToteIconButton
import app.tote.ui.components.ToteIllustration
import app.tote.ui.components.ToteTextField
import app.tote.ui.components.pressScale
import app.tote.ui.icons.EntryIconBadge
import app.tote.ui.theme.ToteShapes
import app.tote.ui.theme.ToteSpacing
import app.tote.ui.theme.Tuck
import app.tote.util.AutofillSetup
import app.tote.util.BackupFiles
import app.tote.util.TileAddResult
import app.tote.util.TilePrompt
import app.tote.util.rememberAutofillStatus

@Composable
fun OnboardingRoute(onDone: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val autofillStatus = rememberAutofillStatus()
    var tileResult by rememberSaveable { mutableStateOf<TileAddResult?>(null) }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.restore(uri)
    }
    LaunchedEffect(state.finished) { if (state.finished) onDone() }
    BackHandler(enabled = state.step != OnboardingStep.WELCOME) { viewModel.back() }
    OnboardingScreen(
        state = state,
        onBack = { viewModel.back() },
        onStart = { viewModel.go(OnboardingStep.PICK) },
        onRestore = { openBackup.launch(BackupFiles.OPEN_TYPES) },
        onToggle = viewModel::toggle,
        onContinuePick = { viewModel.go(OnboardingStep.FILL) },
        onSkipPick = { viewModel.go(OnboardingStep.AUTOFILL) },
        onValue = viewModel::setValue,
        onSaveValues = viewModel::saveValues,
        autofillStatus = autofillStatus,
        onEnableAutofill = { context.startActivity(AutofillSetup.requestIntent(context)) },
        tilePromptSupported = TilePrompt.isSupported,
        tileResult = tileResult,
        onAddTile = { TilePrompt.request(context) { tileResult = it } },
        onFinish = viewModel::finish,
    )
}

/**
 * Onboarding (Plan §8.4, redesign board 6). Four steps behind one progress bar: welcome, pick
 * your essentials, fill them in, turn on autofill. Every step can be skipped.
 */
@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onRestore: () -> Unit,
    onToggle: (String) -> Unit,
    onContinuePick: () -> Unit,
    onSkipPick: () -> Unit,
    onValue: (String, String) -> Unit,
    onSaveValues: () -> Unit,
    autofillStatus: AutofillSetup.Status,
    onEnableAutofill: () -> Unit,
    tilePromptSupported: Boolean,
    tileResult: TileAddResult?,
    onAddTile: () -> Unit,
    onFinish: () -> Unit,
) {
    val c = Tuck.colors
    Box(Modifier.fillMaxSize().background(c.background), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .fillMaxSize()
                .widthIn(max = ToteSpacing.maxContentWidth)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            ProgressHeader(state.step, onBack)
            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    (slideInHorizontally(tween(320)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(220)))
                        .togetherWith(slideOutHorizontally(tween(260)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(160)))
                },
                modifier = Modifier.weight(1f),
                label = "onboardingStep",
            ) { step ->
                when (step) {
                    OnboardingStep.WELCOME -> WelcomeStep(state, onStart, onRestore)
                    OnboardingStep.PICK -> PickStep(state, onToggle, onContinuePick, onSkipPick)
                    OnboardingStep.FILL -> FillStep(state, onValue, onSaveValues)
                    OnboardingStep.AUTOFILL -> AutofillStep(autofillStatus, onEnableAutofill, tilePromptSupported, tileResult, onAddTile, onFinish)
                }
            }
        }
    }
}

@Composable
private fun ProgressHeader(step: OnboardingStep, onBack: () -> Unit) {
    val c = Tuck.colors
    val total = OnboardingStep.entries.size
    val current = step.ordinal + 1
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, end = ToteSpacing.screen, top = 8.dp).defaultMinSize(minHeight = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (step == OnboardingStep.WELCOME) {
            Spacer(Modifier.width(12.dp))
        } else {
            ToteIconButton(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", onClick = onBack)
            Spacer(Modifier.width(4.dp))
        }
        Row(
            Modifier.weight(1f).clearAndSetSemantics { contentDescription = "Step $current of $total" },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(total) { i ->
                val color by animateColorAsState(if (i < current) c.accent else c.surfaceHigh, tween(300), label = "progress$i")
                Box(Modifier.weight(1f).height(4.dp).clip(ToteShapes.pill).background(color))
            }
        }
        Spacer(Modifier.width(12.dp))
        Text("$current of $total", style = Tuck.type.bodySmall, color = c.textSecondary, modifier = Modifier.clearAndSetSemantics { })
    }
}

/** A step: scrolling content above, actions pinned to the bottom. */
@Composable
private fun StepLayout(
    title: String,
    subtitle: String?,
    actions: @Composable () -> Unit,
    top: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val c = Tuck.colors
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ToteSpacing.screen)
                .padding(bottom = 16.dp),
        ) {
            top?.invoke()
            Text(
                title,
                style = Tuck.type.headline.copy(fontSize = Tuck.type.headline.fontSize * 1.2f, lineHeight = Tuck.type.headline.lineHeight * 1.17f),
                color = c.textPrimary,
                modifier = Modifier.padding(top = 22.dp).semantics { heading() },
            )
            if (subtitle != null) {
                Text(subtitle, style = Tuck.type.body, color = c.textSecondary, modifier = Modifier.padding(top = 10.dp))
            }
            content()
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = ToteSpacing.screen).padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) { actions() }
    }
}

private val bigButton = PaddingValues(horizontal = 24.dp, vertical = 17.dp)

@Composable
private fun WelcomeStep(state: OnboardingState, onStart: () -> Unit, onRestore: () -> Unit) {
    val c = Tuck.colors
    StepLayout(
        title = "Stop retyping the same things",
        subtitle = "Tuck keeps the links and answers you put into every form, and fills them in for you.",
        top = {
            TuckWordmark(Modifier.padding(top = 20.dp))
            Box(Modifier.fillMaxWidth().padding(top = 28.dp), contentAlignment = Alignment.Center) { ToteIllustration() }
        },
        actions = {
            if (state.restoreError != null) {
                Text(
                    state.restoreError,
                    style = Tuck.type.bodySmall,
                    color = c.danger,
                    modifier = Modifier.padding(bottom = 6.dp).semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            PillButton("Get started", onClick = onStart, enabled = !state.busy, modifier = Modifier.fillMaxWidth(), contentPadding = bigButton)
            PillButton("Restore from a backup", onClick = onRestore, enabled = !state.busy, style = PillStyle.Ghost, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        },
    ) {
        Column(Modifier.padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Point("Suggestions right in the form, in apps and Chrome")
            Point("One tap to copy from your Quick Settings")
            Point("Stays on this phone. Tuck can't use the internet.")
        }
    }
}

@Composable
private fun Point(text: String) {
    val c = Tuck.colors
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 2.dp).size(20.dp).background(c.accentSoft, ToteShapes.pill), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = c.accentContent, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = Tuck.type.body, color = c.textPrimary)
    }
}

@Composable
private fun PickStep(state: OnboardingState, onToggle: (String) -> Unit, onContinue: () -> Unit, onSkip: () -> Unit) {
    val count = state.selected.size
    StepLayout(
        title = "What do you type into forms most?",
        subtitle = "Pick a few. Next you'll paste in the values, and Tuck will recognise these fields everywhere.",
        actions = {
            PillButton(
                if (count == 0) "Pick at least one" else "Continue with $count",
                onClick = onContinue,
                enabled = count > 0,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = bigButton,
            )
            PillButton("I'll add my own later", onClick = onSkip, style = PillStyle.Quiet, modifier = Modifier.fillMaxWidth())
        },
    ) {
        Column(Modifier.padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Two columns break words like "LinkedIn" at large text sizes, so go to one.
            val columns = if (LocalDensity.current.fontScale >= 1.4f) 1 else 2
            state.templates.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { t ->
                        TemplateTile(t, selected = t.key in state.selected, onToggle = { onToggle(t.key) }, modifier = Modifier.weight(1f).fillMaxHeight())
                    }
                    if (row.size < columns) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TemplateTile(t: SeedTemplate, selected: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val c = Tuck.colors
    val bg by animateColorAsState(if (selected) c.accentTint else c.surface, label = "tileBg")
    val border by animateColorAsState(if (selected) c.accent else c.outline, label = "tileBorder")
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .pressScale(interaction)
            .clip(ToteShapes.field)
            .background(bg)
            .border(1.5.dp, border, ToteShapes.field)
            .toggleable(selected, interaction, ripple(), role = Role.Checkbox, onValueChange = { onToggle() })
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntryIconBadge(t.icon, size = 32.dp)
        Spacer(Modifier.width(10.dp))
        Text(t.label, style = Tuck.type.titleSmall.copy(fontSize = Tuck.type.body.fontSize * 0.94f), color = c.textPrimary, modifier = Modifier.weight(1f))
        if (selected) {
            Box(Modifier.size(22.dp).background(c.accent, ToteShapes.pill), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun FillStep(state: OnboardingState, onValue: (String, String) -> Unit, onSave: () -> Unit) {
    val filled = state.filledCount
    val list = state.selectedTemplates
    StepLayout(
        title = "Paste in your details",
        subtitle = "Fill in what you have. Blank ones are skipped, and you can change anything later.",
        actions = {
            PillButton(
                when (filled) {
                    0 -> "Skip for now"
                    1 -> "Save 1 entry"
                    else -> "Save $filled entries"
                },
                onClick = onSave,
                enabled = !state.busy,
                style = if (filled == 0) PillStyle.Secondary else PillStyle.Primary,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = bigButton,
            )
        },
    ) {
        Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            list.forEachIndexed { i, t ->
                val long = t.type == EntryType.LONG_TEXT
                ToteTextField(
                    value = state.values[t.key].orEmpty(),
                    onValueChange = { onValue(t.key, it) },
                    label = t.label,
                    placeholder = t.placeholder,
                    singleLine = !long,
                    minLines = if (long) 3 else 1,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = when (t.type) {
                            EntryType.EMAIL -> KeyboardType.Email
                            EntryType.PHONE -> KeyboardType.Phone
                            EntryType.LINK -> KeyboardType.Uri
                            else -> KeyboardType.Text
                        },
                        imeAction = if (i == list.lastIndex || long) ImeAction.Default else ImeAction.Next,
                    ),
                )
            }
        }
    }
}

@Composable
private fun AutofillStep(
    status: AutofillSetup.Status,
    onEnable: () -> Unit,
    tilePromptSupported: Boolean,
    tileResult: TileAddResult?,
    onAddTile: () -> Unit,
    onFinish: () -> Unit,
) {
    val c = Tuck.colors
    val on = status == AutofillSetup.Status.ENABLED
    StepLayout(
        title = "Let Tuck fill forms for you",
        subtitle = "When you tap a field like \"LinkedIn profile\", Tuck suggests the right entry above the keyboard.",
        actions = {
            PillButton(if (on || status == AutofillSetup.Status.UNSUPPORTED) "Open Tuck" else "Not now", onClick = onFinish, style = if (on || status == AutofillSetup.Status.UNSUPPORTED) PillStyle.Primary else PillStyle.Quiet, modifier = Modifier.fillMaxWidth(), contentPadding = bigButton)
        },
    ) {
        Column(
            Modifier
                .padding(top = 22.dp)
                .fillMaxWidth()
                .clip(ToteShapes.cardLarge)
                .background(c.accentTint)
                .border(1.dp, c.accentTintBorder, ToteShapes.cardLarge)
                .padding(18.dp),
        ) {
            when (status) {
                AutofillSetup.Status.ENABLED -> Row(
                    Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(44.dp).background(c.accent, ToteShapes.badge), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Autofill is on", style = Tuck.type.title, color = c.textPrimary)
                        Text("Tuck will suggest entries in forms", style = Tuck.type.bodySmall, color = c.textSecondary)
                    }
                }
                AutofillSetup.Status.UNSUPPORTED -> Text(
                    "This phone doesn't support autofill services. You can still copy entries from Tuck and the Quick Settings tile.",
                    style = Tuck.type.body,
                    color = c.textPrimary,
                )
                AutofillSetup.Status.DISABLED -> {
                    Text(
                        "Android allows one autofill service at a time. If a password manager fills forms for you now, Tuck takes its place. You can switch back in Settings.",
                        style = Tuck.type.bodySmall,
                        color = c.textSecondary,
                    )
                    PillButton("Turn on autofill", onClick = onEnable, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), contentPadding = bigButton)
                }
            }
            if (status != AutofillSetup.Status.UNSUPPORTED) {
                Text(
                    "Using Chrome? It also needs \"Autofill using another service\" in Chrome's own settings. Tuck's Settings walks you through it.",
                    style = Tuck.type.bodySmall,
                    color = c.textSecondary,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        }

        if (tilePromptSupported) {
            val added = tileResult == TileAddResult.ADDED || tileResult == TileAddResult.ALREADY_ADDED
            Row(
                Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .clip(ToteShapes.card)
                    .background(c.surface)
                    .defaultMinSize(minHeight = 64.dp)
                    .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Quick Settings tile", style = Tuck.type.body, color = c.textPrimary)
                    Text("Copy an entry from the notification shade", style = Tuck.type.bodySmall, color = c.textSecondary)
                }
                Spacer(Modifier.width(8.dp))
                if (added) {
                    Row(
                        Modifier.background(c.surfaceHigh, ToteShapes.pill).padding(start = 8.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = c.accentContent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Added", style = Tuck.type.label, color = c.textPrimary)
                    }
                } else {
                    PillButton("Add", onClick = onAddTile, icon = Icons.Rounded.Add, style = PillStyle.Secondary, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp))
                }
            }
        }
    }
}
