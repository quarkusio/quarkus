package io.quarkus.cache.kotlin.runtime

import io.quarkus.cache.CacheResult
import io.quarkus.cache.runtime.AbstractCache
import io.quarkus.cache.runtime.CacheSpecialMethodHandler
import io.smallrye.mutiny.Uni
import jakarta.interceptor.InvocationContext
import java.lang.reflect.Method
import java.time.Duration
import java.util.function.Function
import java.util.function.Supplier
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Special method handling for Kotlin {@code suspend} functions annotated with [CacheResult].
 *
 * <p>
 * Registered through [io.quarkus.cache.runtime.CacheRecorder] by the cache-kotlin extension so that
 * the caching interceptors stay free of a Kotlin dependency.
 */
class CacheKotlinSuspendMethodHandler : CacheSpecialMethodHandler {

    override fun canHandle(method: Method): Boolean {
        val parameterTypes = method.parameterTypes
        return parameterTypes.isNotEmpty() &&
            Continuation::class.java.isAssignableFrom(parameterTypes[parameterTypes.size - 1])
    }

    override fun handleCacheResult(
        invocationContext: InvocationContext,
        cache: AbstractCache,
        key: Any?,
        binding: CacheResult,
    ): Any? {
        /*
         * A Kotlin suspend function must never be treated as a synchronous method: blocking while waiting for the
         * cache value would fail on a Vert.x event loop thread. The cache value computation and the resumption of
         * the calling coroutine are both non-blocking. The caller's Continuation is captured before the value
         * computation starts because the computation replaces the Continuation held by the invocation context.
         */
        val parameters = invocationContext.parameters
        val continuation = parameters[parameters.size - 1]
        var cacheValue =
            cache
                .getAsync(key, Function { invokeSuspending(invocationContext) })
                .onFailure()
                .recoverWithUni(
                    Function { failure: Throwable ->
                        /*
                         * A failed computation is removed from the cache before the calling coroutine observes the
                         * failure, so that an immediate retry triggers a new computation.
                         */
                        cache.invalidate(key).replaceWith(Uni.createFrom().failure(failure))
                    }
                )
        if (binding.lockTimeout > 0) {
            // IMPORTANT: The item/failure are emitted on the captured context.
            cacheValue =
                cacheValue
                    .ifNoItem()
                    .after(Duration.ofMillis(binding.lockTimeout))
                    .recoverWithUni(Supplier { invokeSuspending(invocationContext) })
        }
        return resumeSuspended(continuation, cacheValue)
    }

    private fun invokeSuspending(invocationContext: InvocationContext): Uni<Any?> {
        return Uni.createFrom().emitter { emitter ->
            val parameters = invocationContext.parameters.clone()
            val continuationIndex = parameters.size - 1

            @Suppress("UNCHECKED_CAST")
            val callerContinuation = parameters[continuationIndex] as Continuation<Any?>
            parameters[continuationIndex] =
                object : Continuation<Any?> {
                    override val context: CoroutineContext
                        get() = callerContinuation.context

                    override fun resumeWith(result: Result<Any?>) {
                        // Duplicate terminal events are ignored by the emitter, so no guard is
                        // needed for the
                        // "completed without suspending" case below.
                        result.fold({ emitter.complete(it) }, { emitter.fail(it) })
                    }
                }
            invocationContext.setParameters(parameters)
            try {
                val result = invocationContext.proceed()
                if (result !== COROUTINE_SUSPENDED) {
                    emitter.complete(result)
                }
            } catch (e: Throwable) {
                emitter.fail(e)
            }
        }
    }

    private fun resumeSuspended(continuation: Any?, value: Uni<Any?>): Any? {
        @Suppress("UNCHECKED_CAST") val callerContinuation = continuation as Continuation<Any?>
        value
            .subscribe()
            .with(
                { item -> callerContinuation.resume(item) },
                { failure -> callerContinuation.resumeWithException(failure) },
            )
        return COROUTINE_SUSPENDED
    }
}
