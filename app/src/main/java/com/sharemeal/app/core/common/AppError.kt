package com.sharemeal.app.core.common

sealed class AppError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Network(message: String = "Network unavailable.", cause: Throwable? = null) : AppError(message, cause)
    class Unauthorized(message: String = "Session expired.") : AppError(message)
    class Forbidden(message: String = "You are not allowed to perform this action.") : AppError(message)
    class NotFound(message: String = "The requested item was not found.") : AppError(message)
    class Validation(
        val fieldErrors: Map<String, String> = emptyMap(),
        message: String = "Please check the information provided."
    ) : AppError(message)
    class Conflict(message: String) : AppError(message)
    class Unknown(message: String = "Something went wrong.", cause: Throwable? = null) : AppError(message, cause)
}
