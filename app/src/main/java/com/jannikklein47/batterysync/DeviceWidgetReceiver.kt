package com.jannikklein47.batterysync

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class DeviceWidgetReceiver : GlanceAppWidgetReceiver() {

    // This tells the system which GlanceWidget to display
    override val glanceAppWidget: GlanceAppWidget = DeviceWidget()

    // Optional: You can override onUpdate or onReceive if you want
    // to perform specific actions when the widget is refreshed.
}