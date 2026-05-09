package com.gowmaz.blevel.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.gowmaz.blevel.R
import com.gowmaz.blevel.orientation.Orientation

/**
 * Main application screen.
 * Implements an immersive, high-fidelity UI for the Bubble Level tool.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: LevelViewModel,
    onSettingsClick: () -> Unit,
    onCalibrateClick: () -> Unit,
    onRulerToggle: (Boolean) -> Unit,
    onCycleOrientation: () -> Unit,
    isRulerShowing: Boolean
) {
    val isLocked by viewModel.isLocked
    val currentOrientation by viewModel.orientation
    
    val context = LocalContext.current
    val sharedPreferences = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    
    var showRulerCalib by remember { mutableStateOf(false) }
    var rulerCal by remember { mutableIntStateOf(sharedPreferences.getInt("pref_rulercal", 100)) }
    var rulerCoarseCal by remember { mutableIntStateOf(sharedPreferences.getInt("pref_rulercoarsecal", 2000)) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.name).uppercase(),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            letterSpacing = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                tonalElevation = 3.dp
            ) {
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        if (isRulerShowing) showRulerCalib = !showRulerCalib else onCalibrateClick()
                    },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_calibrate),
                            contentDescription = stringResource(R.string.calibrate)
                        )
                    },
                    label = { Text(stringResource(R.string.calibrate)) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = if (showRulerCalib) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = if (showRulerCalib) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                
                NavigationBarItem(
                    selected = isRulerShowing,
                    onClick = { onRulerToggle(!isRulerShowing) },
                    icon = {
                        Icon(
                            painter = painterResource(if (isRulerShowing) R.drawable.ic_bubble else R.drawable.ic_ruler),
                            contentDescription = stringResource(R.string.toggle_mode)
                        )
                    },
                    label = { Text(if (isRulerShowing) "Bubble" else "Ruler") }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onSettingsClick,
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { viewModel.toggleLock() }
                )
        ) {
            // High-fidelity rendering surface
            if (isRulerShowing) {
                val dpmm = calculateDpmm(context, rulerCal, rulerCoarseCal)
                RulerCanvas(
                    dpmm = dpmm.toDouble(),
                    dpfi = dpmm * 25.4 / 32,
                    modifier = Modifier.fillMaxSize().semantics { contentDescription = "Ruler Tool" }
                )
                
                if (showRulerCalib) {
                    RulerCalibrationOverlay(
                        fineValue = rulerCal.toFloat(),
                        coarseValue = rulerCoarseCal.toFloat(),
                        onFineChange = { 
                            rulerCal = it.toInt()
                            sharedPreferences.edit().putInt("pref_rulercal", rulerCal).apply()
                        },
                        onCoarseChange = {
                            rulerCoarseCal = it.toInt()
                            sharedPreferences.edit().putInt("pref_rulercoarsecal", rulerCoarseCal).apply()
                        },
                        onReset = {
                            rulerCal = 100
                            rulerCoarseCal = 2000
                            sharedPreferences.edit().putInt("pref_rulercal", 100).putInt("pref_rulercoarsecal", 2000).apply()
                        }
                    )
                }
            } else {
                LevelCanvas(
                    viewModel = viewModel, 
                    isLocked = isLocked,
                    modifier = Modifier.fillMaxSize().semantics { contentDescription = "Bubble Level Tool" }
                )
            }

            // Mode switching button when locked
            // Rotation is corrected to ensure button content is upright
            val overlayRotation = when(currentOrientation) {
                Orientation.LEFT -> -90f
                Orientation.RIGHT -> 90f
                Orientation.BOTTOM -> 180f
                else -> 0f
            }

            AnimatedVisibility(
                visible = isLocked,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = onCycleOrientation,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.graphicsLayer { rotationZ = overlayRotation }
                ) {
                    Icon(painterResource(R.drawable.ic_bubble), contentDescription = "Switch Mode")
                }
            }
        }
    }
}

/**
 * Overlay for fine-tuning ruler calibration.
 */
@Composable
fun RulerCalibrationOverlay(
    fineValue: Float,
    coarseValue: Float,
    onFineChange: (Float) -> Unit,
    onCoarseChange: (Float) -> Unit,
    onReset: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val context = LocalContext.current
        
        // Fine Control (Left)
        Column(
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 32.dp).fillMaxHeight().width(64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(painterResource(R.drawable.ic_fine), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            VerticalSlider(
                value = fineValue,
                onValueChange = onFineChange,
                valueRange = 0f..200f,
                modifier = Modifier.weight(1f).semantics { contentDescription = "Fine Calibration" }
            )
        }

        // Coarse Control (Right)
        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 32.dp).fillMaxHeight().width(64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(painterResource(R.drawable.ic_coarse), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            VerticalSlider(
                value = coarseValue,
                onValueChange = onCoarseChange,
                valueRange = 0f..6000f,
                modifier = Modifier.weight(1f).semantics { contentDescription = "Coarse Calibration" }
            )
        }

        // Reset Action
        IconButton(
            onClick = onReset,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                .minimumInteractiveComponentSize()
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_reset), 
                contentDescription = stringResource(R.string.reset), 
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Custom slider oriented vertically using graphics transformations.
 */
@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f
) {
    androidx.compose.material3.Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier
            .graphicsLayer {
                rotationZ = 270f
            }
            .layout { measurable: Measurable, constraints: Constraints ->
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxWidth
                    )
                )
                layout(placeable.height, placeable.width) {
                    placeable.place(-placeable.width, 0)
                }
            }
    )
}

private fun calculateDpmm(context: android.content.Context, progress: Int, coarseProgress: Int): Float {
    return (context.resources.displayMetrics.ydpi / 25.4f) * (1 + (progress + coarseProgress - 100f - 2000f) / 5000f)
}
