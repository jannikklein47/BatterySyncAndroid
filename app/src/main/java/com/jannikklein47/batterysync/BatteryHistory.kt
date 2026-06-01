package com.jannikklein47.batterysync

data class BatteryHistory(var day: List<BatteryHistoryEntry>, var week: List<BatteryHistoryEntry>)