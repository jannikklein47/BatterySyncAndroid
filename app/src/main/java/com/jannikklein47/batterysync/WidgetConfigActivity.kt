package com.jannikklein47.batterysync

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.stringPreferencesKey // or intPreferencesKey, etc.
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.ButtonDefaults
import androidx.glance.appwidget.CheckboxDefaults
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class WidgetConfigActivity : ComponentActivity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Get the Widget ID from the Intent
        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // 2. If it's invalid, kill the activity
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        var initialShowPercent = true
        var initialAlwaysFirst = true
        var initialDarkMode: Boolean? = null
        var initialTransparency = 0.94f

        CoroutineScope(Dispatchers.IO).launch {
            initialShowPercent = DataStoreManager(applicationContext).getWidgetShowPercent()
            initialAlwaysFirst = DataStoreManager(applicationContext).getWidgetAlwaysFirst()
            initialDarkMode = DataStoreManager(applicationContext).getWidgetDarkMode()
            initialTransparency = DataStoreManager(applicationContext).getWidgetTransparency().toFloat() / 100f
        }



        setContent {
            // State to track the checkbox
            var showPercent by remember { mutableStateOf(initialShowPercent) }
            var alwaysFirst by remember { mutableStateOf(initialAlwaysFirst) }
            var darkMode by remember { mutableStateOf(initialDarkMode) }
            var transparency by remember { mutableFloatStateOf(initialTransparency) }

                // Surface provides the "Card" look with elevation and rounded corners
            Surface(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(28.dp), // Standard M3 Dialog rounding
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Widget Einstellungen",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPercent = !showPercent }
                            .padding(vertical = 8.dp)
                    ) {
                        Checkbox(
                            checked = showPercent,
                            onCheckedChange = { showPercent = it },
                            colors = androidx.compose.material3.CheckboxDefaults.colors(
                                checkedColor = Color(0xFF3B71CA), // Material Blue
                                uncheckedColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "%-Symbol nach Akkustand")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { alwaysFirst = !alwaysFirst }
                            .padding(vertical = 8.dp)
                    ) {
                        Checkbox(
                            checked = alwaysFirst,
                            onCheckedChange = { alwaysFirst = it },
                            colors = androidx.compose.material3.CheckboxDefaults.colors(
                                checkedColor = Color(0xFF3B71CA), // Material Blue
                                uncheckedColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Dieses Gerät immer als erstes zeigen")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(text = "Widget-Design", color = Color.Black)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        TextButton(onClick = {darkMode = null}) {
                            Text("System", color = if (darkMode == null) Color.Black else Color.Gray)
                        }
                        TextButton(onClick = {darkMode = false}) {
                            Text(text = "Hell", color = if (darkMode == false) Color.Black else Color.Gray)
                        }
                        TextButton(onClick = {darkMode = true}) {
                            Text(text = "Dunkel", color = if (darkMode == true) Color.Black else Color.Gray)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(text = "Widget-Transparenz: ${(transparency * 100).toInt()}%", color = Color.Black)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Slider(
                            value = transparency,
                            onValueChange = { value ->
                                transparency = value
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF3B71CA),
                                activeTrackColor = Color(0xFF3B71CA)
                            )
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        TextButton(onClick = {transparency = 0.94f}) {
                            Text("Zurücksetzen", color = Color.Gray)
                        }
                    }

                    Text(
                        text = "Hinweis: Es kann eine Weile dauern, bis das Widget sich an die neuen Einstellungen anpasst, um Energie zu sparen.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        // "Cancel" feel: Just finish without saving
                        TextButton(onClick = { finish() }) {
                            Text("Abbrechen", color = Color(0xFF3B71CA))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3B71CA)
                            ),
                            onClick = { saveAndFinish(showPercent, alwaysFirst, darkMode, transparency) }) {
                            Text("Speichern")
                        }
                    }
                }
            }

        }
    }

    private fun saveAndFinish(showPercent: Boolean, alwaysFirst: Boolean, darkMode: Boolean?, transparency: Float) {
        val context = this
        lifecycleScope.launch {
            // 3. Save your settings using GlanceStateDefinition (DataStore)

            DataStoreManager(context).saveWidgetShowPercent(showPercent)
            DataStoreManager(context).saveWidgetAlwaysFirst(alwaysFirst)
            DataStoreManager(context).saveWidgetDarkMode(darkMode)
            DataStoreManager(context).saveWidgetTransparency((transparency * 100f).toInt())

            // 4. Update the widget immediately
            notifyWidgets(context)

            // 5. Success! Tell the system to place the widget
            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}