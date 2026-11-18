package com.sharemeal.app.data.sync

interface SyncWorkScheduler {
    fun scheduleImmediate()
    fun schedulePeriodic()
}
