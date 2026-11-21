package app.tote.feature.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.runtime.remember
import android.content.Intent
import app.tote.feature.playground.AutofillPlaygroundActivity
import app.tote.util.AutofillSetup
import app.tote.util.rememberAutofillStatus
import androidx.compose.material.icons.rounded.Check
import app.tote.ui.components.PillButton
import app.tote.ui.components.PillStyle
import app.tote.util.TileAddResult
import app.tote.util.TilePrompt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.tote.BuildConfig
import app.tote.core.data.settings.AppSettings
import app.tote.core.data.settings.SettingsRepository
import app.tote.core.data.settings.ThemeMode
import app.tote.ui.components.ToteIconButton
import app.tote.ui.icons.ToteIcons
import app.tote.ui.theme.Tote
import app.tote.ui.theme.ToteShapes
import app.tote.ui.theme.ToteSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<AppSettings> = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    fun setTheme(mode: ThemeMode) { viewModelScope.launch { settings.setThemeMode(mode) } }
    fun setDynamic(enabled: Boolean) { viewModelScope.launch { settings.setDynamicColor(enabled) } }
}

@Composable
fun SettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val autofillStatus = rememberAutofillStatus()
    val chrome = remember { AutofillSetup.chromeLaunchIntent(context) }
    var tileResult by rememberSaveable { mutableStateOf<TileAddResult?>(null) }
    SettingsScreen(
        state = state,
        onBack = onBack,
        onTheme = { viewModel.setTheme(it) },
        onDynamic = { viewModel.setDynamic(it) },
        tilePromptSupported = TilePrompt.isSupported,
        tileResult = tileResult,
        onAddTile = { TilePrompt.request(context) { tileResult = it } },
        autofillStatus = autofillStatus,
        onEnableAutofill = { context.startActivity(AutofillSetup.requestIntent(context)) },
        onManageAutofill = { context.startActivity(AutofillSetup.manageIntent()) },
        onOpenChrome = chrome?.let { intent -> { context.startActivity(intent) } },
        onTryAutofill = { context.startActivity(Intent(context, AutofillPlaygroundActivity::class.java)) },
    )
}

@Composable
fun SettingsScreen(
    state: AppSettings,
    onBack: () -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onDynamic: (Boolean) -> Unit,
    tilePromptSupported: Boolean = false,
    tileResult: TileAddResult? = null,
    onAddTile: () -> Unit = {},
    autofillStatus: AutofillSetup.Status = AutofillSetup.Status.DISABLED,
    onEnableAutofill: () -> Unit = {},
    onManageAutofill: () -> Unit = {},
    onOpenChrome: (() -> Unit)? = null,
    onTryAutofill: () -> Unit = {},
) {
    val c = Tote.colors
    Box(Modifier.fillMaxSize().background(c.background), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .fillMaxSize()
                .widthIn(max = ToteSpacing.maxContentWidth)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
        ) {
            ToteIconButton(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
            )
            Text(
                "Settings",
                style = Tote.type.display,
                color = c.textPrimary,
                modifier = Modifier
                    .padding(horizontal = ToteSpacing.screen, vertical = 12.dp)
                    .semantics { heading() },
            )

            SectionLabel("Appearance")
            Card {
                Text("Theme", style = Tote.type.titleSmall, color = c.textPrimary)
                Spacer(Modifier.height(12.dp))
                ThemeSelector(state.themeMode, onTheme)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(state.dynamicColor, role = Role.Switch, onValueChange = onDynamic)
                            .defaultMinSize(minHeight = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Dynamic colour", style = Tote.type.titleSmall, color = c.textPrimary)
                            Text("Use your wallpaper's colours instead of lime", style = Tote.type.bodySmall, color = c.textSecondary)
                        }
                        Switch(
                            checked = state.dynamicColor,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = c.onAccent,
                                checkedTrackColor = c.accent,
                                uncheckedThumbColor = c.textSecondary,
                                uncheckedTrackColor = c.surfaceHigh,
                                uncheckedBorderColor = c.outlineStrong,
                            ),
                        )
                    }
                }
            }

            SectionLabel("Autofill")
            AutofillCard(autofillStatus, onEnableAutofill, onManageAutofill, onTryAutofill)
            Spacer(Modifier.height(12.dp))
            ChromeCard(onOpenChrome)

            SectionLabel("Quick Settings tile")
            Card {
                Text("Copy from anywhere", style = Tote.type.titleSmall, color = c.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Pull down the notification shade, tap Tote, pick an entry. It's copied, ready to paste in any app.",
                    style = Tote.type.bodySmall,
                    color = c.textSecondary,
                )
                Spacer(Modifier.height(14.dp))
                if (tilePromptSupported) {
                    PillButton(
                        text = if (tileResult == TileAddResult.ADDED || tileResult == TileAddResult.ALREADY_ADDED) "Tile added" else "Add the tile",
                        onClick = onAddTile,
                        icon = if (tileResult == TileAddResult.ADDED || tileResult == TileAddResult.ALREADY_ADDED) Icons.Rounded.Check else Icons.Rounded.Add,
                        style = if (tileResult == TileAddResult.ADDED || tileResult == TileAddResult.ALREADY_ADDED) PillStyle.Secondary else PillStyle.Primary,
                    )
                    val note = when (tileResult) {
                        TileAddResult.ALREADY_ADDED -> "It's already in your Quick Settings."
                        TileAddResult.NOT_ADDED -> "Not added. You can also add it by editing your Quick Settings."
                        TileAddResult.UNAVAILABLE -> "Your phone didn't show the prompt. Add it by editing your Quick Settings."
                        else -> null
                    }
                    if (note != null) {
                        Text(note, style = Tote.type.bodySmall, color = c.textSecondary, modifier = Modifier.padding(top = 10.dp))
                    }
                } else {
                    TileSteps()
                }
            }

            SectionLabel("About")
            Card {
                Text("Tote ${BuildConfig.VERSION_NAME}", style = Tote.type.titleSmall, color = c.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Everything stays on this phone. Tote has no internet permission, no accounts and no analytics.",
                    style = Tote.type.bodySmall,
                    color = c.textSecondary,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Set in Geist and Geist Mono (SIL Open Font License). Brand marks from Simple Icons (CC0).",
                    style = Tote.type.bodySmall,
                    color = c.textTertiary,
                )
            }
        }
    }
}

@Composable
private fun AutofillCard(
    status: AutofillSetup.Status,
    onEnable: () -> Unit,
    onManage: () -> Unit,
    onTry: () -> Unit,
) {
    val c = Tote.colors
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Suggest entries in forms", style = Tote.type.titleSmall, color = c.textPrimary)
                Text(
                    "Tap a field like \"LinkedIn profile\" and Tote offers the right link, in apps and in your browser.",
                    style = Tote.type.bodySmall,
                    color = c.textSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            StatusPill(status)
        }
        Spacer(Modifier.height(14.dp))
        when (status) {
            AutofillSetup.Status.DISABLED -> {
                PillButton("Turn on autofill", onClick = onEnable, icon = ToteIcons.FieldFill)
                Text(
                    "Android uses one autofill service at a time. Choosing Tote turns off your password manager's autofill until you switch back. The Quick Settings tile works either way.",
                    style = Tote.type.bodySmall,
                    color = c.textSecondary,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            AutofillSetup.Status.ENABLED -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("Try it", onClick = onTry, icon = Icons.Rounded.Edit)
                PillButton("Change service", onClick = onManage, style = PillStyle.Ghost)
            }
            AutofillSetup.Status.UNSUPPORTED -> Text(
                "This phone doesn't offer autofill to other apps. Use the Quick Settings tile instead.",
                style = Tote.type.bodySmall,
                color = c.textSecondary,
            )
        }
    }
}

@Composable
private fun StatusPill(status: AutofillSetup.Status) {
    val c = Tote.colors
    val on = status == AutofillSetup.Status.ENABLED
    Row(
        Modifier
            .background(if (on) c.accent else c.surfaceRaised, ToteShapes.pill)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon + word, so the state never rests on colour alone.
        Icon(if (on) Icons.Rounded.Check else Icons.Rounded.Close, contentDescription = null, tint = if (on) c.onAccent else c.textSecondary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(if (on) "On" else "Off", style = Tote.type.label, color = if (on) c.onAccent else c.textSecondary)
    }
}

/** Chrome needs its own switch before it hands web forms to Tote (Plan §9.7). */
@Composable
private fun ChromeCard(onOpenChrome: (() -> Unit)?) {
    val c = Tote.colors
    Card {
        Text("In Chrome", style = Tote.type.titleSmall, color = c.textPrimary)
        Text(
            "Chrome fills web forms itself unless you tell it to use Tote.",
            style = Tote.type.bodySmall,
            color = c.textSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        Steps(
            listOf(
                "In Chrome, open Settings, then Autofill services.",
                "Choose \"Autofill using another service\".",
                "Restart Chrome when it asks.",
            ),
        )
        if (onOpenChrome != null) {
            Spacer(Modifier.height(14.dp))
            PillButton("Open Chrome", onClick = onOpenChrome, style = PillStyle.Secondary, icon = Icons.AutoMirrored.Rounded.OpenInNew)
        }
    }
}

@Composable
private fun Steps(steps: List<String>) {
    val c = Tote.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        steps.forEachIndexed { i, step ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(24.dp)
                        .background(c.accentSoft, ToteShapes.pill),
                    contentAlignment = Alignment.Center,
                ) { Text("${i + 1}", style = Tote.type.label, color = c.accentContent) }
                Spacer(Modifier.width(12.dp))
                Text(step, style = Tote.type.bodySmall, color = c.textPrimary, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

/** Manual steps for Android 12 and older, which have no "add tile" prompt. */
@Composable
private fun TileSteps() = Steps(
    listOf(
        "Swipe down twice from the top to open Quick Settings.",
        "Tap the pencil (edit) button.",
        "Drag Tote into your tiles.",
    ),
)

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = Tote.type.overline,
        color = Tote.colors.textSecondary,
        modifier = Modifier
            .padding(start = ToteSpacing.screen + 4.dp, top = 24.dp, bottom = 10.dp)
            .semantics { heading() },
    )
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    val c = Tote.colors
    Column(
        Modifier
            .padding(horizontal = ToteSpacing.screen)
            .fillMaxWidth()
            .clip(ToteShapes.card)
            .background(c.surface)
            .border(1.dp, c.outline, ToteShapes.card)
            .padding(18.dp),
    ) { content() }
}

/** Segmented control: System / Dark / Light. */
@Composable
private fun ThemeSelector(mode: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val c = Tote.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.surfaceRaised, ToteShapes.pill)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ThemeMode.entries.forEach { m ->
            val selected = m == mode
            Box(
                Modifier
                    .weight(1f)
                    .clip(ToteShapes.pill)
                    .background(if (selected) c.accent else c.surfaceRaised)
                    .selectable(selected, role = Role.RadioButton, onClick = { onSelect(m) })
                    .defaultMinSize(minHeight = 44.dp)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    when (m) {
                        ThemeMode.SYSTEM -> "System"
                        ThemeMode.DARK -> "Dark"
                        ThemeMode.LIGHT -> "Light"
                    },
                    style = Tote.type.label,
                    color = if (selected) c.onAccent else c.textPrimary,
                )
            }
        }
    }
}
