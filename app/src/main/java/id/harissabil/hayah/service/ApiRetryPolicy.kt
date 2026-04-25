package id.harissabil.hayah.service

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Retries transient network failures (timeouts, IO, 5xx, 429) using exponential backoff.
 */
suspend fun <T> executeWithNetworkRetry(
    maxAttempts: Int = 3,
    initialDelayMs: Long = 400L,
    maxDelayMs: Long = 2_000L,
    block: suspend () -> T,
): T {
    require(maxAttempts > 0) { "maxAttempts must be > 0" }

    var currentDelay = initialDelayMs
    var lastError: Throwable? = null

    for (attempt in 1..maxAttempts) {
        try {
            return block()
        } catch (e: Throwable) {
            if (e is CancellationException && e !is TimeoutCancellationException) {
                throw e
            }

            lastError = e
            val shouldRetry = attempt < maxAttempts && e.isRetriableNetworkError()
            if (!shouldRetry) {
                throw e
            }

            delay(currentDelay)
            currentDelay = (currentDelay * 2).coerceAtMost(maxDelayMs)
        }
    }

    throw lastError ?: IllegalStateException("Retry failed without captured error")
}

private fun Throwable.isRetriableNetworkError(): Boolean =
    when (this) {
        is SocketTimeoutException,
        is TimeoutCancellationException,
        is IOException,
        -> true

        is HttpException -> code() == 429 || code() in 500..599
        else -> false
    }
