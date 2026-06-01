package com.jannikklein47.batterysync

data class BatteryDevice(
    val name: String,
    val level: Int,
    val iconRes: Int,
    val chargingStatus: Boolean,
    val isPluggedIn: Boolean
)