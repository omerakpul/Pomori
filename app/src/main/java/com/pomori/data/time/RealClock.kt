package com.pomori.data.time

import com.pomori.domain.Clock
import javax.inject.Inject

class RealClock @Inject constructor() : Clock {
    override fun now(): Long = System.currentTimeMillis()
}
