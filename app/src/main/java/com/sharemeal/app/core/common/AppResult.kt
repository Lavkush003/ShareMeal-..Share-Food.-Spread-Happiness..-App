package com.sharemeal.app.core.common

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }
}
