package app.tote.feature.settings

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.tote.BuildConfig
import app.tote.core.data.repo.LearningRepository
import app.tote.core.data.repo.ProfileRepository
import app.tote.core.data.settings.Accent
import app.tote.core.data.settings.AppSettings
import app.tote.core.data.settings.SettingsRepository
import app.tote.core.data.settings.ThemeMode
import app.tote.feature.playground.AutofillPlaygroundActivity
import app.tote.feature.vault.SheetHandle
import app.tote.ui.components.GroupDivider
import app.tote.ui.components.PillButton
import app.tote.ui.components.PillStyle
import app.tote.ui.components.Segmented
import app.tote.ui.components.ToteIconButton
import app.tote.ui.icons.ToteIcons
import app.tote.ui.theme.ToteShapes
import app.tote.ui.theme.ToteSpacing
import app.tote.ui.theme.TotePalette
import app.tote.ui.theme.Tuck
import app.tote.util.AutofillSetup
import app.tote.util.TileAddResult
import app.tote.util.TilePrompt
import app.tote.util.rememberAutofillStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    profiles: ProfileRepository,
    learning: LearningRepository,
) : ViewModel() {
    val state: StateFlow<AppSettings> = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    /** "Tech is active · 2 profiles" style summary for the Profiles row. */
    val profileSummary: StateFlow<String> = combine(profiles.profiles, settings.settings) { list, s ->
        val active = list.firstOrNull { it.id == s.activeProfileId }?.name ?: "All"
        if (list.isEmpty()) "None yet. Everything is in All." else "$active is active · ${list.size} ${if (list.size == 1) "profile" else "profiles"}"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val learnedCount: StateFlow<Int> = learning.mappings
        .map { rows -> rows.map { it.source to it.signature }.distinct().size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setTheme(mode: ThemeMode) { viewModelScope.launch { settings.setThemeMode(mode) } }
    fun setDynamic(enabled: Boolean) { viewModelScope.launch { settings.setDynamicColor(enabled) } }
    fun setAccent(accent: Accent) { viewModelScope.launch { settings.setAccent(accent) } }
}

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenProfiles: () -> Unit = {},
    onOpenLearned: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profileSummary by viewModel.profileSummary.collectAsStateWithLifecycle()
    val learnedCount by viewModel.learnedCount.collectAsStateWithLifecycle()
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
        profileSummary = profileSummary,
        onOpenProfiles = onOpenProfiles,
        learnedSummary = when (learnedCount) {
            0 -> "Nothing yet"
            1 -> "1 field remembered"
            else -> "$learnedCount fields remembered"
        },
        onOpenLearned = onOpenLearned,
        onAccent = { viewModel.setAccent(it) },
    )
}

/**
 * Settings (Plan §8.4, redesign): autofill status first, as a hero card; then your data, quick
 * access, appearance (theme, accent, wallpaper colours) and about, each as a grouped list.
 */
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
    profileSummary: String = "",
    onOpenProfiles: () -> Unit = {},
    learnedSummary: String = "",
    onOpenLearned: () -> Unit = {},
    onAccent: (Accent) -> Unit = {},
) {
    val c = Tuck.colors
    var chromeSheet by remember { mutableStateOf(false) }
    var tileSheet by remember { mutableStateOf(false) }
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
                style = Tuck.type.display,
                color = c.textPrimary,
                modifier = Modifier
                    .padding(horizontal = ToteSpacing.screen, vertical = 8.dp)
                    .semantics { heading() },
            )

            AutofillHero(
                status = autofillStatus,
                onEnable = onEnableAutofill,
                onManage = onManageAutofill,
                onTry = onTryAutofill,
                onChrome = { chromeSheet = true },
                modifier = Modifier.padding(horizontal = ToteSpacing.screen, vertical = 12.dp),
            )

            SectionLabel("Your data")
            Group {
                NavRow("Profiles", profileSummary, onOpenProfiles)
                GroupDivider(inset = 16)
                NavRow("Learned suggestions", learnedSummary, onOpenLearned)
            }

            SectionLabel("Quick access")
            Group {
                val added = tileResult == TileAddResult.ADDED || tileResult == TileAddResult.ALREADY_ADDED
                Row(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 64.dp)
                        .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Quick Settings tile", style = Tuck.type.body, color = c.textPrimary)
                        Text(
                            when (tileResult) {
                                TileAddResult.NOT_ADDED -> "Not added. You can also add it by editing Quick Settings."
                                TileAddResult.UNAVAILABLE -> "Add it by editing your Quick Settings."
                                else -> "Copy from the notification shade, in any app"
                            },
                            style = Tuck.type.bodySmall,
                            color = c.textSecondary,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    when {
                        added -> Row(
                            Modifier.background(c.surfaceHigh, ToteShapes.pill).padding(start = 8.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = c.accentContent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Added", style = Tuck.type.label, color = c.textPrimary)
                        }
                        tilePromptSupported -> PillButton("Add", onClick = onAddTile, icon = Icons.Rounded.Add, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp))
                        else -> PillButton("How", onClick = { tileSheet = true }, style = PillStyle.Secondary, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp))
                    }
                }
            }

            SectionLabel("Appearance")
            Group(Modifier.padding(bottom = 4.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Segmented(
                        options = ThemeMode.entries,
                        selected = state.themeMode,
                        label = {
                            when (it) {
                                ThemeMode.SYSTEM -> "System"
                                ThemeMode.DARK -> "Dark"
                                ThemeMode.LIGHT -> "Light"
                            }
                        },
                        onSelect = onTheme,
                        track = c.background,
                    )
                    Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 18.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Accent", style = Tuck.type.body, color = c.textPrimary, modifier = Modifier.weight(1f).semantics { heading() })
                        Text(
                            if (state.dynamicColor) "From your wallpaper" else state.accent.label(),
                            style = Tuck.type.bodySmall,
                            color = c.textSecondary,
                        )
                    }
                    AccentPicker(selected = state.accent, enabled = !state.dynamicColor, onSelect = onAccent)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    GroupDivider(inset = 16)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(state.dynamicColor, role = Role.Switch, onValueChange = onDynamic)
                            .defaultMinSize(minHeight = 64.dp)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Use wallpaper colours", style = Tuck.type.body, color = c.textPrimary)
                            Text("Overrides the accent", style = Tuck.type.bodySmall, color = c.textSecondary)
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

            SectionLabel("About")
            Group {
                Column(Modifier.padding(16.dp)) {
                    Text("Tuck ${BuildConfig.VERSION_NAME}", style = Tuck.type.body, color = c.textPrimary)
                    Text(
                        "Everything stays on this phone. Tuck has no internet permission, no accounts and no analytics.",
                        style = Tuck.type.bodySmall,
                        color = c.textSecondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        "Set in Geist and Geist Mono (SIL Open Font License). Brand marks from Simple Icons (CC0), icons from Material Symbols (Apache 2.0).",
                        style = Tuck.type.bodySmall,
                        color = c.textTertiary,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }
    }

    if (chromeSheet) {
        StepsSheet(
            title = "Use Tuck in Chrome",
            intro = "Chrome fills web forms itself unless you tell it to use Tuck.",
            steps = listOf(
                "In Chrome, open Settings, then Autofill services.",
                "Choose \"Autofill using another service\".",
                "Restart Chrome when it asks.",
            ),
            action = onOpenChrome?.let { open -> "Open Chrome" to open },
            onDismiss = { chromeSheet = false },
        )
    }
    if (tileSheet) {
        StepsSheet(
            title = "Add the Tuck tile",
            intro = "Your Android version needs the tile added by hand.",
            steps = listOf(
                "Swipe down twice from the top to open Quick Settings.",
                "Tap the pencil (edit) button.",
                "Drag Tuck into your tiles.",
            ),
            action = null,
            onDismiss = { tileSheet = false },
        )
    }
}

private fun Accent.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

/** The autofill status, as the first thing in Settings: on (tinted, with a check) or off. */
@Composable
private fun AutofillHero(
    status: AutofillSetup.Status,
    onEnable: () -> Unit,
    onManage: () -> Unit,
    onTry: () -> Unit,
    onChrome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Tuck.colors
    val on = status == AutofillSetup.Status.ENABLED
    Column(
        modifier
            .fillMaxWidth()
            .clip(ToteShapes.cardLarge)
            .background(if (on) c.accentTint else c.surface)
            .border(1.dp, if (on) c.accentTintBorder else c.outline, ToteShapes.cardLarge)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).background(if (on) c.accent else c.surfaceHigh, ToteShapes.field),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (on) Icons.Rounded.Check else ToteIcons.FieldFill,
                    contentDescription = null,
                    tint = if (on) c.onAccent else c.textPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when (status) {
                        AutofillSetup.Status.ENABLED -> "Autofill is on"
                        AutofillSetup.Status.DISABLED -> "Autofill is off"
                        AutofillSetup.Status.UNSUPPORTED -> "Autofill isn't available"
                    },
                    style = Tuck.type.title,
                    color = c.textPrimary,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    when (status) {
                        AutofillSetup.Status.ENABLED -> "Suggesting entries in apps and Chrome"
                        AutofillSetup.Status.DISABLED -> "Tap a field like \"LinkedIn profile\" and Tuck offers the right link"
                        AutofillSetup.Status.UNSUPPORTED -> "This phone doesn't offer autofill to other apps. The Quick Settings tile still works."
                    },
                    style = Tuck.type.bodySmall,
                    color = c.textSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        when (status) {
            AutofillSetup.Status.ENABLED -> Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("Try it", onClick = onTry, modifier = Modifier.weight(1f))
                PillButton("Chrome setup", onClick = onChrome, style = PillStyle.Ghost, modifier = Modifier.weight(1f))
            }
            AutofillSetup.Status.DISABLED -> Column(Modifier.padding(top = 16.dp)) {
                PillButton("Turn on autofill", onClick = onEnable, icon = ToteIcons.FieldFill, modifier = Modifier.fillMaxWidth())
                Text(
                    "Android uses one autofill service at a time. Choosing Tuck turns off your password manager's autofill until you switch back. The Quick Settings tile works either way.",
                    style = Tuck.type.bodySmall,
                    color = c.textSecondary,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            AutofillSetup.Status.UNSUPPORTED -> Unit
        }
        if (on) {
            Text(
                "Change autofill service",
                style = Tuck.type.label,
                color = c.textSecondary,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clip(ToteShapes.pill)
                    .clickable(role = Role.Button, onClick = onManage)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            )
        }
    }
}

/**
 * Accent swatches. The selected one has a ring and a check, and its name shows beside "Accent",
 * so the choice never rests on colour alone.
 */
@Composable
private fun AccentPicker(selected: Accent, enabled: Boolean, onSelect: (Accent) -> Unit) {
    val c = Tuck.colors
    Row(
        Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Accent.entries.forEach { accent ->
            val spec = TotePalette.accents.getValue(accent)
            val fill = if (c.isDark) spec.darkFill else spec.lightFill
            val on = if (c.isDark) spec.darkOn else spec.lightOn
            val isSelected = accent == selected && enabled
            Column(
                Modifier
                    .clip(ToteShapes.field)
                    .selectable(isSelected, enabled = enabled, role = Role.RadioButton, onClick = { onSelect(accent) })
                    .padding(horizontal = 2.dp, vertical = 4.dp)
                    .semantics { contentDescription = "${accent.label()} accent" },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .border(2.dp, if (isSelected) c.textPrimary else Color.Transparent, ToteShapes.pill)
                        .padding(4.dp)
                        .background(fill.copy(alpha = if (enabled) 1f else 0.4f), ToteShapes.pill)
                        .border(1.dp, c.outline, ToteShapes.pill),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) Icon(Icons.Rounded.Check, contentDescription = null, tint = on, modifier = Modifier.size(20.dp))
                }
                Text(
                    accent.label(),
                    style = Tuck.type.overline.copy(
                        letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    color = if (isSelected) c.textPrimary else c.textSecondary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun StepsSheet(title: String, intro: String, steps: List<String>, action: Pair<String, () -> Unit>?, onDismiss: () -> Unit) {
    val c = Tuck.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = ToteShapes.sheet,
        containerColor = c.surface,
        dragHandle = { SheetHandle() },
    ) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text(title, style = Tuck.type.title, color = c.textPrimary, modifier = Modifier.padding(top = 8.dp).semantics { heading() })
            Text(intro, style = Tuck.type.bodySmall, color = c.textSecondary, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                steps.forEachIndexed { i, step ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(Modifier.size(26.dp).background(c.accentSoft, ToteShapes.pill), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", style = Tuck.type.label, color = c.accentContent)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(step, style = Tuck.type.body, color = c.textPrimary, modifier = Modifier.padding(top = 1.dp))
                    }
                }
            }
            if (action != null) {
                PillButton(
                    action.first,
                    onClick = action.second,
                    icon = Icons.AutoMirrored.Rounded.OpenInNew,
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = Tuck.type.overline,
        color = Tuck.colors.textSecondary,
        modifier = Modifier
            .padding(start = ToteSpacing.screen + 4.dp, top = 24.dp, bottom = 10.dp)
            .semantics { heading() },
    )
}

@Composable
private fun Group(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = Tuck.colors
    Column(
        modifier
            .padding(horizontal = ToteSpacing.screen)
            .fillMaxWidth()
            .clip(ToteShapes.card)
            .background(c.surface),
    ) { content() }
}

@Composable
private fun NavRow(title: String, detail: String, onClick: () -> Unit) {
    val c = Tuck.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = 64.dp)
            .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Tuck.type.body, color = c.textPrimary)
            if (detail.isNotEmpty()) Text(detail, style = Tuck.type.bodySmall, color = c.textSecondary)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textTertiary)
    }
}
