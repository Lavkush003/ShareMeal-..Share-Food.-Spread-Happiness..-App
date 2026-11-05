package com.sharemeal.app.core.common

import kotlinx.coroutines.CoroutineDispatcher

data class DispatcherProvider(
    val io: CoroutineDispatcher,
    val default: CoroutineDispatcher,
    val main: CoroutineDispatcher
)
