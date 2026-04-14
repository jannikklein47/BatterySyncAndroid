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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

        CoroutineScope(Dispatchers.IO).launch {
            initialShowPercent = DataStoreManager(applicationContext).getWidgetShowPercent()
        }



        setContent {
            // State to track the checkbox
            var showPercent by remember { mutableStateOf(initialShowPercent) }

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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            onCheckedChange = { showPercent = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "%-Symbol nach Akkustand")
                    }

                    Text(
                        text = "Hinweis: Es kann bis zu 50 Sekunden dauern, bis das Widget sich an die neuen Einstellungen anpasst, um Energie zu sparen.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        // "Cancel" feel: Just finish without saving
                        TextButton(onClick = { finish() }) {
                            Text("Abbrechen")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { saveAndFinish(showPercent) }) {
                            Text("Speichern")
                        }
                    }
                }
            }

        }
    }

    private fun saveAndFinish(showPercent: Boolean) {
        val context = this
        lifecycleScope.launch {
            // 3. Save your settings using GlanceStateDefinition (DataStore)

            DataStoreManager(context).saveWidgetShowPercent(showPercent)

            // 4. Update the widget immediately
            DeviceWidget().updateAll(context)

            // 5. Success! Tell the system to place the widget
            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}