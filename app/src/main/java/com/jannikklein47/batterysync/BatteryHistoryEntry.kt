package com.jannikklein47.batterysync

import java.time.Instant

data class BatteryHistoryEntry(val createdAt: Instant, val battery: Int)