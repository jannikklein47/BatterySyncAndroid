package com.jannikklein47.batterysync

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.BatteryManager
import android.util.Log
import android.widget.RemoteViews
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.*
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.datastore.preferences.core.Preferences
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import org.json.JSONArray

class DeviceWidget : GlanceAppWidget() {

    // THIS IS THE MOST IMPORTANT LINE FOR RESIZING
    override val sizeMode = SizeMode.Exact

    val dynamicBackground = ColorProvider(
        day = Color(0xF0D9D9D9), // Light gray from your Figma
        night = Color(0xF01C1C1E) // Standard iOS/Android dark gray
    )

    val brightDynamicBackground = ColorProvider(
        day = Color(0xF0D9D9D9), // Light gray from your Figma
        night = Color(0xF0D9D9D9) // Standard iOS/Android dark gray
    )

    val darkDynamicBackground = ColorProvider(
        day = Color(0xF01C1C1E), // Light gray from your Figma
        night = Color(0xF01C1C1E) // Standard iOS/Android dark gray
    )
    val batteryFontColor = ColorProvider(
        day = Color(0xFF000000),
        night = Color(0xFFFFFFFF)
    )

    val brightBatteryFontColor = ColorProvider(
        day = Color(0xFF000000),
        night = Color(0xFF000000)
    )

    val darkBatteryFontColor = ColorProvider(
        day = Color(0xFFFFFFFF),
        night = Color(0xFFFFFFFF)
    )

    val deviceFontColor = ColorProvider(
        day = Color(0xFF606060),
        night = Color(0xFFB0B0B0)
    )

    val brightDeviceFontColor = ColorProvider(
        day = Color(0xFF606060),
        night = Color(0xFF606060)
    )

    val darkDeviceFontColor = ColorProvider(
        day = Color(0xFFB0B0B0),
        night = Color(0xFFB0B0B0)
    )

    // Helper to keep the logic clean
    private fun parseDevicesFromJson(jsonString: String, localId: Int?, localName: String?, alwaysFirst: Boolean, context: Context): List<BatteryDevice> {
        Log.d("widget", "Parsing devices")
        if (jsonString.isEmpty()) return emptyList()
        var thisDeviceDisplayed = false

        val deviceList = ArrayList<BatteryDevice>()
        val deviceListFirstLocal = ArrayList<BatteryDevice>()
        try {
            val jsonArray = JSONArray(jsonString)


            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getInt("id")
                if (id == localId && obj.optBoolean("isShown", false) && !alwaysFirst) {
                    thisDeviceDisplayed = true
                    val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                        context.registerReceiver(null, ifilter)
                    }

                    if (batteryStatus != null) {
                        val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                        val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                        val batteryPct = (level / scale.toDouble())

                        val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING

                        val chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                        val isPluggedIn = chargePlug == BatteryManager.BATTERY_PLUGGED_AC ||
                                chargePlug == BatteryManager.BATTERY_PLUGGED_USB ||
                                chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS

                        deviceList.add(
                            BatteryDevice(
                                name = obj.getString("name"),
                                level = (batteryPct * 100f).toInt(),
                                iconRes = R.drawable.ic_phone,
                                chargingStatus = isCharging,
                                isPluggedIn = isPluggedIn
                            )
                        )
                    } else {
                        BatteryDevice(
                            name = obj.getString("name"),
                            level = (obj.getDouble("battery") * 100f).toInt(),
                            // Map the icon based on your logic or a string from JSON
                            iconRes = if (obj.getString("type").contains("laptop"))
                                R.drawable.ic_laptop else R.drawable.ic_phone,
                            chargingStatus = obj.getBoolean("chargingStatus"),
                            isPluggedIn = obj.getBoolean("isPluggedIn")
                            // id is also available
                        )
                    }

                } else if (obj.optBoolean("isShown", true) && id != localId) {
                    deviceList.add(
                        BatteryDevice(
                            name = obj.getString("name"),
                            level = (obj.getDouble("battery") * 100f).toInt(),
                            // Map the icon based on your logic or a string from JSON
                            iconRes = if (obj.getString("type").contains("laptop"))
                                R.drawable.ic_laptop else R.drawable.ic_phone,
                            chargingStatus = obj.getBoolean("chargingStatus"),
                            isPluggedIn = obj.getBoolean("isPluggedIn")
                            // id is also available
                        )
                    )
                }
            }

            if (!thisDeviceDisplayed || alwaysFirst) {
                Log.d("DeviceWidget", "Adding this devices to the first of the list")
                val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                    context.registerReceiver(null, ifilter)
                }

                if (batteryStatus != null) {
                    val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val batteryPct = (level / scale.toDouble())

                    val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING

                    val chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                    val isPluggedIn = chargePlug == BatteryManager.BATTERY_PLUGGED_AC ||
                            chargePlug == BatteryManager.BATTERY_PLUGGED_USB ||
                            chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS

                    deviceListFirstLocal.add(BatteryDevice(
                        name = if (localName.isNullOrEmpty()) "Dieses Gerät" else localName,
                        level = (batteryPct * 100f).toInt(),
                        iconRes = R.drawable.ic_phone,
                        chargingStatus = isCharging,
                        isPluggedIn = isPluggedIn
                    ))
                    deviceListFirstLocal.addAll(deviceList)



                } else Log.d("DeviceWidget", "Could not get battery status")
            }
        } catch (e: Exception) {
            Log.e("BatteryWidget", "Error parsing JSON", e)
        }
        val displayedList = mutableListOf<BatteryDevice>()
        if (thisDeviceDisplayed) displayedList.addAll(deviceList) else displayedList.addAll(deviceListFirstLocal)

        return displayedList
    }


    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            DeviceWidgetContent(context)
        }
    }
    fun createBatteryBitmap(context: Context, level: Int, chargingStatus: Boolean, isPluggedIn: Boolean): Bitmap {
        val size = 180 // px
        // Ensure you use Bitmap.createBitmap with a Config
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)

        val stroke = 25f
        // The margin should be at least half the strokeWidth to avoid clipping
        val margin = stroke / 2f + 2f

        val paint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
        }

        // This defines the outer bounds of the circle
        // Left: margin, Top: margin, Right: size - margin, Bottom: size - margin
        val left = margin
        val top = margin
        val right = size - margin
        val bottom = size - margin

        // Draw background gray arc (Using a lighter gray to match Figma better)
        paint.color = 0xFF828282.toInt()
        canvas.drawArc(left, top, right, bottom, 145f, 250f, false, paint)

        // Draw progress arc
        paint.color = getBatteryColor(level, chargingStatus, isPluggedIn) // Ensure this returns an Int
        val sweepAngle = (level / 100f) * 250f
        canvas.drawArc(left, top, right, bottom, 145f, sweepAngle, false, paint)

        return bitmap
    }

    fun getBatteryColor(level: Int, chargingStatus: Boolean, isPluggedIn: Boolean): Int {
        if (chargingStatus) return (0xFF006EFF).toInt()
        if (isPluggedIn) return (0xFF568bd1).toInt()
        return when {
            level > 30 -> (0xFF009900).toInt()
            level > 15 -> (0xFFEECC00).toInt()
            else -> (0xFFCC0000).toInt()
        }
    }

    @Composable
    private fun DeviceWidgetContent(context: Context) {
        // Get Device List from Store

        val prefs = currentState<Preferences>()
        val jsonString = prefs[WidgetKeys.JSON]
        val localId = prefs[WidgetKeys.LOCAL_ID]
        val localName = prefs[WidgetKeys.LOCAL_NAME]
        val localBatteryJSON = prefs[WidgetKeys.LOCAL_BATTERY_JSON]
        val alwaysFirst = if (prefs[WidgetKeys.ALWAYS_FIRST] != null) prefs[WidgetKeys.ALWAYS_FIRST].toBoolean() else false
        val showPercent = if(prefs[WidgetKeys.SHOW_PERCENT] != null) prefs[WidgetKeys.SHOW_PERCENT].toBoolean() else true
        val darkMode = if (prefs[WidgetKeys.DARK_MODE] == null || prefs[WidgetKeys.DARK_MODE].equals("null")) null else prefs[WidgetKeys.DARK_MODE].toBoolean()
        val transparency = ((prefs[WidgetKeys.TRANSPARENCY]?.toFloat()) ?: ((240.toFloat() / 255.toFloat()) * 100f)) / 100f

        Log.d("Widget", "transparency: $transparency")

        val dynamicBackground = ColorProvider(
            day = Color(0xF0D9D9D9).copy(alpha = transparency), // Light gray from your Figma
            night = Color(0xF01C1C1E).copy(alpha = transparency) // Standard iOS/Android dark gray
        )

        val brightDynamicBackground = ColorProvider(
            day = Color(0xF0D9D9D9).copy(alpha = transparency), // Light gray from your Figma
            night = Color(0xF0D9D9D9).copy(alpha = transparency) // Standard iOS/Android dark gray
        )

        val darkDynamicBackground = ColorProvider(
            day = Color(0xF01C1C1E).copy(alpha = transparency), // Light gray from your Figma
            night = Color(0xF01C1C1E).copy(alpha = transparency) // Standard iOS/Android dark gray
        )

        Log.d("Widget", "Darkmode: $darkMode")

        val data = remember(jsonString, alwaysFirst, localName, localId, darkMode, localBatteryJSON) {
            if (!jsonString.isNullOrEmpty()) {
                try {
                    parseDevicesFromJson(
                        jsonString,
                        localId?.toInt(),
                        localName,
                        alwaysFirst,
                        context
                    )
                } catch (e: Exception) { null }
            } else { null }
        }



        val size = LocalSize.current
        val width = size.width
        val height = size.height
        val scale = if (height < 100.dp) 0.8f else 1.0f

        // Map width (dp) to number of columns
        // 1x1 is usually < 100dp, 2x1 is ~150dp, 3x1 is ~220dp, 4x1 is > 280dp
        val maxDevices = when {
            width >= 280.dp -> 4
            width >= 200.dp -> 3
            width >= 120.dp -> 2
            else -> 1 // 1x1 mode
        }

        // Take only the amount of devices that fit the current size
        val visibleDevices = data?.take(maxDevices) ?: listOf()

        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(8.dp)
                .background(
                    if (darkMode == true) darkDynamicBackground else if (darkMode == false) brightDynamicBackground else dynamicBackground,
                )

                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            visibleDevices.forEach { device ->
                Column(
                    modifier = GlanceModifier.defaultWeight(), // THIS creates the equal grid
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // For the ring, Glance supports CircularProgressIndicator
                        Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.padding(bottom = 20.dp).fillMaxSize()) {
                            Image(
                                provider = ImageProvider(createBatteryBitmap(context, device.level, device.chargingStatus, device.isPluggedIn)),
                                contentDescription = null,
                                modifier = GlanceModifier.size(56.dp)
                            )
                            Image(
                                provider = ImageProvider(device.iconRes),
                                contentDescription = null,
                                modifier = GlanceModifier.size(20.dp),
                                colorFilter = androidx.glance.ColorFilter.tint(if (darkMode == true) darkBatteryFontColor else if (darkMode == false) brightBatteryFontColor else batteryFontColor)
                            )
                        }

                        Column(
                            modifier = GlanceModifier.fillMaxSize().padding(top = (48 * scale).toInt().dp),
                            verticalAlignment = Alignment.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${device.level}${if (showPercent) "%" else ""}",
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (darkMode == true) darkBatteryFontColor else if (darkMode == false) brightBatteryFontColor else batteryFontColor
                                ),
                            )
                            Text(
                                text = device.name,
                                style = TextStyle(fontSize = 9.sp, color = if (darkMode == true) darkDeviceFontColor else if (darkMode == false) brightDeviceFontColor else deviceFontColor, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium),
                                maxLines = 1
                            )
                        }

                    }
                }
            }
        }
    }
}