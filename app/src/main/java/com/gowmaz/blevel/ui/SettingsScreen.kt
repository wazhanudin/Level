package com.gowmaz.blevel.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.gowmaz.blevel.BuildConfig
import com.gowmaz.blevel.R
import com.gowmaz.blevel.util.PrefKeys

/**
 * Settings screen for configuring application behavior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sharedPreferences = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    
    // Explicit state to track preference changes and force UI sync
    var prefUpdateTrigger by remember { mutableIntStateOf(0) }
    var showChangelog by remember { mutableStateOf(false) }
    
    val listener = remember {
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            prefUpdateTrigger++
        }
    }

    DisposableEffect(sharedPreferences) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.preferences)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(android.R.string.cancel))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            item {
                val isChecked = remember(prefUpdateTrigger) {
                    sharedPreferences.getBoolean(context.getString(PrefKeys.PREF_SHOW_ANGLE), true)
                }
                PreferenceSwitch(
                    title = stringResource(R.string.preference_show_angle),
                    summary = stringResource(R.string.preference_show_angle_summary),
                    checked = isChecked,
                    onCheckedChange = { 
                        sharedPreferences.edit().putBoolean(context.getString(PrefKeys.PREF_SHOW_ANGLE), it).apply() 
                    }
                )
            }

            item {
                val offset = remember(prefUpdateTrigger) {
                    sharedPreferences.getInt("pref_offsetAngle", 0).toFloat()
                }
                PreferenceSlider(
                    title = stringResource(R.string.offset_angle),
                    initialValue = offset,
                    valueRange = 0f..45f,
                    onValueFinished = { 
                        sharedPreferences.edit().putInt("pref_offsetAngle", it.toInt()).apply() 
                    }
                )
            }

            item {
                val showAngle = remember(prefUpdateTrigger) {
                    sharedPreferences.getBoolean(context.getString(PrefKeys.PREF_SHOW_ANGLE), true)
                }
                val currentValue = remember(prefUpdateTrigger) {
                    sharedPreferences.getString(context.getString(PrefKeys.PREF_DISPLAY_TYPE), context.getString(PrefKeys.PREF_DISPLAY_TYPE_ANGLE)) ?: ""
                }
                PreferenceList(
                    title = stringResource(R.string.preference_display_type),
                    entries = stringArrayResource(R.array.pref_displayTypes_options),
                    entryValues = stringArrayResource(R.array.pref_displayTypes_values),
                    value = currentValue,
                    enabled = showAngle,
                    onValueChange = { 
                        sharedPreferences.edit().putString(context.getString(PrefKeys.PREF_DISPLAY_TYPE), it).apply() 
                    }
                )
            }

            item {
                val viscosity = remember(prefUpdateTrigger) {
                    sharedPreferences.getString(context.getString(PrefKeys.PREF_VISCOSITY), context.getString(PrefKeys.PREF_VISCOSITY_MEDIUM)) ?: ""
                }
                PreferenceList(
                    title = stringResource(R.string.preference_viscosity),
                    entries = stringArrayResource(R.array.pref_viscosity_options),
                    entryValues = stringArrayResource(R.array.pref_viscosity_values),
                    value = viscosity,
                    onValueChange = { 
                        sharedPreferences.edit().putString(context.getString(PrefKeys.PREF_VISCOSITY), it).apply() 
                    }
                )
            }

            item {
                val soundEnabled = remember(prefUpdateTrigger) {
                    sharedPreferences.getBoolean(context.getString(PrefKeys.PREF_ENABLE_SOUND), false)
                }
                PreferenceSwitch(
                    title = stringResource(R.string.preference_sound),
                    summary = stringResource(R.string.preference_sound_summary),
                    checked = soundEnabled,
                    onCheckedChange = { 
                        sharedPreferences.edit().putBoolean(context.getString(PrefKeys.PREF_ENABLE_SOUND), it).apply() 
                    }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                ListItem(
                    headlineContent = { Text("GitHub") },
                    supportingContent = { Text("View source and license") },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                    modifier = Modifier.clickable(role = Role.Button) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/wazhanudin/Level")))
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Version") },
                    supportingContent = { Text(BuildConfig.VERSION_NAME) },
                    leadingContent = { Icon(painterResource(R.drawable.ic_info), contentDescription = null) },
                    modifier = Modifier.clickable(role = Role.Button) { showChangelog = true }
                )
            }
        }
    }

    if (showChangelog) {
        AlertDialog(
            onDismissRequest = { showChangelog = false },
            title = { Text("Changelog - v${BuildConfig.VERSION_NAME}") },
            text = {
                Text("UI/UX Modernization\n- Full Jetpack Compose migration\n- Material 3 Design implementation\n- Modern Spirit Level realistic rendering\n- Immersive edge-to-edge support\n- Enhanced accessibility (WCAG)\n- Performance optimizations\n- Orientation locking improvements\n- Tap-to-lock interaction")
            },
            confirmButton = {
                TextButton(onClick = { showChangelog = false }) { Text("OK") }
            }
        )
    }
}

@Composable
fun PreferenceSwitch(title: String, summary: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        trailingContent = {
            Switch(
                checked = checked, 
                onCheckedChange = onCheckedChange, 
                modifier = Modifier.semantics { contentDescription = title }
            )
        },
        modifier = Modifier.clickable(role = Role.Switch, onClick = { onCheckedChange(!checked) })
    )
}

@Composable
fun PreferenceSlider(
    title: String, 
    initialValue: Float, 
    valueRange: ClosedFloatingPointRange<Float>, 
    onValueFinished: (Float) -> Unit
) {
    var sliderValue by remember(initialValue) { mutableFloatStateOf(initialValue) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = sliderValue, 
                onValueChange = { sliderValue = it }, 
                onValueChangeFinished = { onValueFinished(sliderValue) }, 
                valueRange = valueRange, 
                modifier = Modifier.weight(1f)
            )
            Text(
                text = sliderValue.toInt().toString(), 
                modifier = Modifier.padding(start = 16.dp), 
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun PreferenceList(title: String, entries: Array<String>, entryValues: Array<String>, value: String, enabled: Boolean = true, onValueChange: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val currentIndex = entryValues.indexOf(value).coerceAtLeast(0)

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(entries[currentIndex]) },
        modifier = Modifier.clickable(enabled = enabled, role = Role.Button) { showDialog = true }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                LazyColumn {
                    items(entries.zip(entryValues)) { (entry, entryValue) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.RadioButton) {
                                    onValueChange(entryValue)
                                    showDialog = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = entryValue == value, onClick = null)
                            Text(entry, modifier = Modifier.padding(start = 16.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}
