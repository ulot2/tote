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
    SettingsScreen(state, onBack, { viewModel.setTheme(it) }, { viewModel.setDynamic(it) })
}

@Composable
fun SettingsScreen(
    state: AppSettings,
    onBack: () -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onDynamic: (Boolean) -> Unit,
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
