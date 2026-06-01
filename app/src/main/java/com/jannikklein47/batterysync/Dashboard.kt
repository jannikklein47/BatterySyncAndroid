package com.jannikklein47.batterysync

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import java.time.Duration
import java.time.OffsetDateTime
import java.time.Instant
import java.time.format.DateTimeParseException

import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke


import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer

import androidx.compose.material3.Text
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.ui.input.pointer.positionChange
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.platform.LocalView
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import androidx.compose.material3.pulltorefresh.PullToRefreshBox


fun getRemainingTimeText(predictedZeroAt: String?): String {
    if (predictedZeroAt.isNullOrBlank()) return "--"

    return try {
        // Parse the ISO string and get the duration until that point
        val targetTime = OffsetDateTime.parse(predictedZeroAt).toInstant()
        val now = Instant.now()
        val duration = Duration.between(now, targetTime)

        // If the battery is already predicted to be dead (or past the time)
        if (duration.isNegative || duration.isZero) {
            return ""
        }

        val days = duration.toDays()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60

        // Dynamically build the string so we don't display "0d" if it's less than a day
        buildString {
            append(" - ")
            if (days > 0) append("${days}d ")
            if (hours > 0 || days > 0) append("${hours}h ") // Keep hours if there are days
            if (days == 0L) append("${minutes}m")
        }
    } catch (e: DateTimeParseException) {
        ""
    }
}


// Farbpalette basierend auf dem Screenshot
val BackgroundDark = Color(0xFF0F1318)
val CardBackgroundNormal = Color(0xFF1E222B)
val CardBackgroundWarn = Color(0xFF2C1B00)
val CardBackgroundLow = Color(0xFF2D1418) // Rötlicher Hintergrund bei niedrigem Akku
val BlueProgress = Color(0xFF3B71CA)
val OrangeProgress = Color(0xFFFF9900)
val RedProgress = Color(0xFFDC3545)
val TextGray = Color(0xFF8A939E)
val DividerColor = Color(0xFF2C313A)

@Composable
fun DeviceDashboardScreen(
    devices: List<MainActivity.Device>,
    localId: Int,
    localDeviceName: String,
    serviceRunning: Boolean,
    userName: String,
    offline: Boolean,
    onRegister: () -> Unit,
    onInherit: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartService: () -> Unit,
    fetchBatteryHistory: (deviceId: Int, callback: (BatteryHistory?) -> Unit) -> Unit,
    refreshAll: (onComplete: () -> Unit) -> Unit
) {
    // Zustände für das Laden und die Haptik
    var isRefreshing by remember { mutableStateOf(false) }
    val view = LocalView.current

    // Die PullToRefreshBox umschließt das gesamte Layout
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            // Haptik bei Beginn: Kurzer mechanischer Klick
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

            refreshAll {
                isRefreshing = false
                // Haptik bei Ende: Spürbarer Bestätigungs-Impuls
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark) // Hintergrund auf die Box legen, damit der Indikator gut aussieht
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()) // Erkennt die Pull-Geste automatisch
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // --- Header-Bereich ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hallo, $userName",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { onOpenSettings() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.settings),
                        contentDescription = "Einstellungen",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            if (!serviceRunning) {
                Button(
                    onClick = { onStartService() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.alert),
                        contentDescription = "Achtung",
                        tint = Color.Red,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Synchronisierung starten",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (offline) {
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x1BFF0000),
                        disabledContainerColor = Color(0x1BFF0000)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = false,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.alert),
                        contentDescription = "Achtung",
                        tint = Color(0xFFB01B1B),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Du bist offline. Dir werden möglicherweise nicht die neuesten Informationen angezeigt.",
                        color = Color(0xFFB01B1B),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- Sektion: Dieses Gerät ---
            if (devices.find { device -> device.id == localId } != null) {
                Text(
                    text = "Dieses Gerät",
                    color = TextGray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                DeviceCard(device = devices.find { device -> device.id == localId } ?: devices.first(), offline = offline, fetchBatteryHistory)
            } else {
                RegistrationCard(offline, { onRegister() }, { onInherit() })
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Sektion: Andere Geräte ---
            if (devices.filter { device -> device.id != localId }.size > 1) {
                Text(
                    text = "Andere Geräte",
                    color = TextGray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                devices.filter { device -> device.id != localId }.forEach { device ->
                    DeviceCard(device = device, offline = offline, fetchBatteryHistory)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(
                modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)
            )
        }
    }
}

@Composable
fun DeviceCard(device: MainActivity.Device, offline: Boolean, fetchBatteryHistory: (deviceId: Int, callback: (BatteryHistory?) -> Unit) -> Unit) {
    // Zustand für die Expansion
    var isExpanded by remember { mutableStateOf(false) }

    // Akku-Wert normalisieren (0.0 bis 1.0 für die ProgressBars)
    val batteryProgress = device.battery.coerceIn(0.0, 1.0).toFloat()
    val isLowBattery = device.battery <= 0.15
    val isMiddleBattery = device.battery <= 0.3

    val timeRemaining = getRemainingTimeText(device.predictedZeroAt)

    val cardBackground = if (isLowBattery) CardBackgroundLow else if (isMiddleBattery) CardBackgroundWarn else CardBackgroundNormal
    val progressBarColor = if (isLowBattery) RedProgress else if (isMiddleBattery) OrangeProgress else BlueProgress

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animiert den Scale-Wert elastisch, sobald gedrückt wird
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f, // 0.96 bedeutet 4% kleiner beim Drücken
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, // Verleiht dem Effekt etwas "Federn"
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "CardPressScale"
    )

    var isLoadingHistory by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(BatteryHistory(day = emptyList(), week = emptyList())) }
    val view = LocalView.current

    LaunchedEffect(device) {
        isExpanded = false
        history = BatteryHistory(day = emptyList(), week = emptyList())
    }

    LaunchedEffect(isExpanded) {
        if (isExpanded) view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        if (isExpanded && (history.day.isEmpty() || history.week.isEmpty())) {
            isLoadingHistory = true
            fetchBatteryHistory(device.id) { result ->
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                if (result != null) history = result
                Log.d("Dashboard", "Haptic Feedback")
                isLoadingHistory = false
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),

        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            // 1. Skalierung auf die Karte anwenden:
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            // 2. Clickable mit der InteractionSource verknüpfen:
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                isExpanded = !isExpanded
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Zeile 1: Name & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = device.name,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "${(device.battery * 100).toInt()}%$timeRemaining",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Zeile 2: Kapselförmige Akku-ProgressBar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .background(Color(0xFF3A3F45), RoundedCornerShape(8.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = batteryProgress)
                        .fillMaxHeight()
                        .background(progressBarColor, RoundedCornerShape(8.dp))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = DividerColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Zeile 3: Akku-Gesundheit & Ladezyklen
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { device.healthScore / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = BlueProgress,
                        strokeWidth = 4.dp,
                        trackColor = DividerColor,
                        strokeCap = StrokeCap.Round,
                    )
                    Text(
                        text = "${device.healthScore}%",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Akku-Gesundheit",
                        color = TextGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                                append("${device.cycles} ")
                            }
                            withStyle(style = SpanStyle(color = TextGray)) {
                                append("Ladezyklen verbraucht.")
                            }
                        },
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                val rotationAngle by animateFloatAsState(
                    targetValue = if (isExpanded) 90f else 0f,
                    // Ein 'tween' sorgt für eine saubere, gleichmäßige Drehung in 300 Millisekunden
                    animationSpec = tween(durationMillis = 300),
                    label = "ChevronRotation"
                )
                Icon(painter = painterResource(R.drawable.chevron_right), "View More", tint = Color.White, modifier = Modifier.rotate(rotationAngle))
            }

            // --- HIER WIRD DIE KARTE ERWEITERT ---
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DividerColor, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Deine neue Komponente (Beispiel-Inhalt, hier einfach austauschen)
                if (!offline) ExpandedDeviceDetailsContent(device = device, isLoadingHistory, history)
                else Text(text = "Detaillierte Nutzungsdaten sind nur mit einer aktiven Internetverbindung verfügbar.", color = Color.Gray)
            }
        }
    }
}

// Beispiel für deine neue, ausgeklappte Komponente
@Composable
fun ExpandedDeviceDetailsContent(device: MainActivity.Device, isLoadingHistory: Boolean, history: BatteryHistory) {

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isLoadingHistory) {
            // Ladeindikator zentrieren, während die API antwortet
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BlueProgress, modifier = Modifier.size(32.dp))
            }
        } else if (history.day.isNotEmpty() || history.week.isNotEmpty()) {
            // Wenn Daten da sind, zeichnen wir den Graphen
            BatteryHistoryGraph(
                history = history.day,
                title = "Tägliche Nutzung",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            BatteryHistoryGraph(
                history = history.week,
                title = "Wöchentliche Nutzung",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )

        } else {
            // Fallback falls die Liste leer zurückkommt
            Text(
                text = "Keine Verlaufsdaten verfügbar.",
                color = TextGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}

@Composable
fun BatteryHistoryGraph(
    history: List<BatteryHistoryEntry>,
    title: String,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val view = LocalView.current

    // Zeit-Formatierer für den schwebenden Text (z.B. "14:35")
    val timeFormatter = remember {
        DateTimeFormatter.ofPattern("dd.MM, HH:mm").withZone(ZoneId.systemDefault())
    }

    LaunchedEffect(history) {
        selectedIndex = null
    }

    Column(modifier = modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
    ) {}) {
        // --- HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = TextGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            if (selectedIndex != null && selectedIndex!! in history.indices) {
                Text(
                    text = "${history[selectedIndex!!].battery}%",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // --- DER INTERAKTIVE GRAPH ---
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(history) {
                    if (history.size < 2) return@pointerInput
                    val labelWidthPx = 32.dp.toPx()

                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val graphWidth = size.width.toFloat() - labelWidthPx
                        val distanceX = graphWidth / (history.size - 1)

                        val initialRelativeX = down.position.x - labelWidthPx
                        val firstIndex = (initialRelativeX / distanceX).roundToInt()
                            .coerceIn(0, history.size - 1)

                        if (firstIndex != selectedIndex) {
                            selectedIndex = firstIndex
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            Log.d("Dashboard", "Haptic Feedback")
                        }

                        horizontalDrag(down.id) { change ->
                            val dragRelativeX = change.position.x - labelWidthPx
                            val nextIndex = (dragRelativeX / distanceX).roundToInt()
                                .coerceIn(0, history.size - 1)

                            if (nextIndex != selectedIndex) {
                                selectedIndex = nextIndex
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                Log.d("Dashboard", "Haptic Feedback")
                            }

                            if (change.positionChange().x != 0f) {
                                change.consume()
                            }
                        }

                        val lastIndex = null
                        if (selectedIndex != lastIndex) {
                            selectedIndex = lastIndex
                            // Ein optionaler, finaler Klick für das "Zurücksnappen"
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            Log.d("Dashboard", "Haptic Feedback")
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val lineColor = TextGray
            val gridLineColor = Color(0xFF2C313A).copy(alpha = 0.4f)
            val labelTextColor = Color(0xFF8A939E)

            val labelWidth = 32.dp.toPx()
            val graphWidth = width - labelWidth

            // ERHÖHTES PADDING OBEN: Schafft Platz für die schwebende Uhrzeit
            val paddingTop = 24.dp.toPx()
            val paddingBottom = 8.dp.toPx()
            val graphHeight = height - paddingTop - paddingBottom

            val y100 = paddingTop
            val y50 = paddingTop + graphHeight / 2
            val y0 = paddingTop + graphHeight

            // Y-Skala & Hilfslinien zeichnen
            val levels = listOf(100 to y100, 50 to y50, 0 to y0)
            levels.forEach { (percentage, yPosition) ->
                drawLine(
                    color = gridLineColor,
                    start = Offset(x = labelWidth, y = yPosition),
                    end = Offset(x = width, y = yPosition),
                    strokeWidth = 1.dp.toPx()
                )

                val textLayoutResult = textMeasurer.measure(
                    text = percentage.toString(),
                    style = TextStyle(color = labelTextColor, fontSize = 10.sp)
                )

                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        x = (labelWidth - textLayoutResult.size.width) / 2,
                        y = yPosition - (textLayoutResult.size.height / 2)
                    )
                )
            }

            // Graphen-Pfade berechnen
            val distanceX = graphWidth / (history.size - 1)
            val strokePath = Path()
            val fillPath = Path()

            history.forEachIndexed { index, entry ->
                val normalizedBattery = entry.battery.coerceIn(0, 100) / 100f
                val x = labelWidth + (index * distanceX)
                val y = y0 - (normalizedBattery * graphHeight)

                if (index == 0) {
                    strokePath.moveTo(x, y)
                    fillPath.moveTo(x, y0)
                    fillPath.lineTo(x, y)
                } else {
                    strokePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }

                if (index == history.size - 1) {
                    fillPath.lineTo(x, y0)
                    fillPath.close()
                }
            }

            // Verlauf & Hauptlinie zeichnen
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent),
                    startY = y100,
                    endY = y0
                )
            )

            drawPath(
                path = strokePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Selektions-Marker & Schwebende Uhrzeit zeichnen
            selectedIndex?.let { index ->
                if (index in history.indices) {
                    val x = labelWidth + (index * distanceX)
                    val entry = history[index]
                    val normalizedBattery = entry.battery.coerceIn(0, 100) / 100f
                    val y = y0 - (normalizedBattery * graphHeight)

                    // Gestrichelte vertikale Linie
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(x, y100),
                        end = Offset(x, y0),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    // --- NEU: SCHWEBENDE UHRZEIT ZEICHNEN ---
                    val timeText = timeFormatter.format(entry.createdAt)
                    val timeLayoutResult = textMeasurer.measure(
                        text = timeText,
                        style = TextStyle(
                            color = TextGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Light
                        )
                    )

                    // Berechnet das X so, dass der Text mittig über der Linie steht,
                    // aber verhindert das Herausrutschen an den Rändern (coerceIn)
                    val timeX = (x - timeLayoutResult.size.width / 2f)
                        .coerceIn(labelWidth, width - timeLayoutResult.size.width)

                    // Positioniert den Text genau 4.dp über der obersten Linie (y100)
                    val timeY = y100 - timeLayoutResult.size.height - 4.dp.toPx()

                    drawText(
                        textLayoutResult = timeLayoutResult,
                        topLeft = Offset(timeX, timeY)
                    )

                    // Punkte auf der Kurve zeichnen
                    drawCircle(
                        color = Color.White,
                        radius = 5.dp.toPx(),
                        center = Offset(x, y)
                    )

                    drawCircle(
                        color = Color.White.copy(alpha = 0.3f),
                        radius = 9.dp.toPx(),
                        center = Offset(x, y),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun RegistrationCard(
    offline: Boolean,
    onRegisterNewDevice: () -> Unit,
    onInheritOldDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackgroundNormal),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Titel der Karte im selben Stil wie die Gerätenamen
            Text(
                text = "Registrierung",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            // Kleiner Beschreibungstext
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Dieses Gerät ist noch nicht mit dem Server verknüpft. " + (if (offline) "Du benötigst eine Internetverbindung, um eine Verknüpfung mit dem Server herzustellen." else "Bitte wähle eine Option:"),
                color = TextGray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Option 1: Gerät Registrieren (Primäre Aktion)
            Button(
                onClick = onRegisterNewDevice,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BlueProgress, disabledContainerColor = BlueProgress.copy(0.4f)),
                shape = RoundedCornerShape(12.dp),
                enabled = !offline,
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(
                    text = "Gerät Registrieren",
                    color = if (offline) Color.Gray else Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Option 2: Altes Gerät übernehmen (Sekundäre Aktion)
            OutlinedButton(
                onClick = onInheritOldDevice,
                modifier = Modifier.fillMaxWidth(),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(DividerColor)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (offline) Color.Gray else Color.White
                ),
                enabled = !offline,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(
                    text = "Altes Gerät übernehmen",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}