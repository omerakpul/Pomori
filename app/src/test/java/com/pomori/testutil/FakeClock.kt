package com.pomori.testutil

import com.pomori.domain.Clock

class FakeClock(var nowMs: Long = 0L) : Clock {
    override fun now(): Long = nowMs

    fun advanceBy(ms: Long) {
        nowMs += ms
    }
}
