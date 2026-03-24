package com.jannikklein47.batterysync

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import android.widget.RemoteViews
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
import org.json.JSONArray

data class BatteryDevice(
    val name: String,
    val level: Int,
    val iconRes: Int,
)

class DeviceWidget : GlanceAppWidget() {

    // THIS IS THE MOST IMPORTANT LINE FOR RESIZING
    override val sizeMode = SizeMode.Exact

    val dynamicBackground = ColorProvider(
        day = Color(0xF0D9D9D9), // Light gray from your Figma
        night = Color(0xF01C1C1E) // Standard iOS/Android dark gray
    )
    val batteryFontColor = ColorProvider(
        day = Color(0xFF000000),
        night = Color(0xFFFFFFFF)
    )
    val deviceFontColor = ColorProvider(
        day = Color(0xFF606060),
        night = Color(0xFFB0B0B0)
    )

    // Helper to keep the logic clean
    private fun parseDevicesFromJson(jsonString: String): List<BatteryDevice> {
        Log.d("widget", "Parsing devices")
        if (jsonString.isEmpty()) return emptyList()

        val deviceList = mutableListOf<BatteryDevice>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.optBoolean("isShown", true)) {
                    deviceList.add(
                        BatteryDevice(
                            name = obj.getString("name"),
                            level = (obj.getDouble("battery") * 100f).toInt(),
                            // Map the icon based on your logic or a string from JSON
                            iconRes = if (obj.getString("name").contains("MacBook"))
                                R.drawable.ic_laptop else R.drawable.ic_phone
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("BatteryWidget", "Error parsing JSON", e)
        }
        return deviceList
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {

        Log.d("widget", "provide glance")
        // 1. Fetch your raw data (DataStore is usually a Flow or suspend)
        val deviceInfoString = DataStoreManager(context).getAllDevices() ?: ""

        // 2. Parse your JSON into your list of BatteryDevice objects
        val devices = parseDevicesFromJson(deviceInfoString)

        // 3. Pass the parsed list to your content
        provideContent {
            DeviceWidgetContent(devices, context)
        }

    }

    fun createBatteryBitmap(context: Context, level: Int): Bitmap {
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
        paint.color = getBatteryColor(level) // Ensure this returns an Int
        val sweepAngle = (level / 100f) * 250f
        canvas.drawArc(left, top, right, bottom, 145f, sweepAngle, false, paint)

        return bitmap
    }

    fun getBatteryColor(level: Int): Int {
        return when {
            level > 30 -> (0xFF009900).toInt()
            level > 15 -> (0xFFEECC00).toInt()
            else -> (0xFFCC0000).toInt()
        }
    }

    @Composable
    private fun DeviceWidgetContent(devices: List<BatteryDevice>, context: Context) {

        val size = LocalSize.current
        val width = size.width

        // Map width (dp) to number of columns
        // 1x1 is usually < 100dp, 2x1 is ~150dp, 3x1 is ~220dp, 4x1 is > 280dp
        val maxDevices = when {
            width >= 280.dp -> 4
            width >= 200.dp -> 3
            width >= 120.dp -> 2
            else -> 1 // 1x1 mode
        }

        // Take only the amount of devices that fit the current size
        val visibleDevices = devices.take(maxDevices)

        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(8.dp)
                .background(
                    dynamicBackground,
                ),
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
                                provider = ImageProvider(createBatteryBitmap(context, device.level)),
                                contentDescription = null,
                                modifier = GlanceModifier.size(56.dp)
                            )
                            Image(
                                provider = ImageProvider(device.iconRes),
                                contentDescription = null,
                                modifier = GlanceModifier.size(20.dp),
                                colorFilter = androidx.glance.ColorFilter.tint(batteryFontColor)
                            )
                        }

                        Column(
                            modifier = GlanceModifier.fillMaxSize().padding(top = 48.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${device.level}%",
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = batteryFontColor
                                ),
                            )
                            Text(
                                text = device.name,
                                style = TextStyle(fontSize = 9.sp, color = deviceFontColor, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium),
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}