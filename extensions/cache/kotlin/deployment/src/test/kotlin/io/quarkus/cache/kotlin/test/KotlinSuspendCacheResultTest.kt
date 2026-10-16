package io.quarkus.cache.kotlin.test

import io.quarkus.cache.CacheKey
import io.quarkus.cache.CacheResult
import io.quarkus.test.QuarkusExtensionTest
import io.vertx.core.Context
import io.vertx.core.Vertx
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * Tests the [CacheResult] annotation on Kotlin suspend functions.
 */
class KotlinSuspendCacheResultTest {

    companion object {
        @RegisterExtension
        val test = QuarkusExtensionTest()
            .withApplicationRoot { jar -> jar.addClass(CachedService::class.java) }
    }

    @Inject
    lateinit var cachedService: CachedService

    @Inject
    lateinit var vertx: Vertx

    @Test
    fun testCacheResult() = runBlocking<Unit> {
        // First call: the method is invoked and its result is cached.
        val value1 = cachedService.cachedValue("key-1")
        assertThat(cachedService.cachedValueInvocations()).isEqualTo(1)

        // Second call with the same argument: the method is not invoked and the cached value is returned. The
        // Continuation instance passed by the Kotlin compiler differs between the two calls and must not prevent
        // the cache hit.
        val value2 = cachedService.cachedValue("key-1")
        assertThat(cachedService.cachedValueInvocations()).isEqualTo(1)
        assertThat(value2).isSameAs(value1)

        // Call with another argument: the method is invoked again and a new value is cached.
        val value3 = cachedService.cachedValue("key-2")
        assertThat(cachedService.cachedValueInvocations()).isEqualTo(2)
        assertThat(value3).isNotSameAs(value1)
    }

    @Test
    fun testDefaultKey() = runBlocking<Unit> {
        // The Continuation is the only parameter of the method, so the default cache key must be used.
        val value1 = cachedService.defaultKeyValue()
        val value2 = cachedService.defaultKeyValue()
        assertThat(cachedService.defaultKeyValueInvocations()).isEqualTo(1)
        assertThat(value2).isSameAs(value1)
    }

    @Test
    fun testCompositeKey() = runBlocking<Unit> {
        val value1 = cachedService.compositeKeyValue("a", 1)
        val value2 = cachedService.compositeKeyValue("a", 1)
        assertThat(value2).isSameAs(value1)

        val value3 = cachedService.compositeKeyValue("a", 2)
        assertThat(value3).isNotSameAs(value1)
    }

    @Test
    fun testExceptionsArePropagatedAndNotCached() = runBlocking<Unit> {
        for (expectedInvocations in 1..2) {
            try {
                cachedService.failing("key")
            } catch (e: IllegalStateException) {
                assertThat(e).hasMessage("expected failure")
            }
            // Failures must not be cached, so each call must invoke the method.
            assertThat(cachedService.failingInvocations()).isEqualTo(expectedInvocations)
        }
    }

    /**
     * Reproduces the scenario from issue #23746: calling a cached suspend function from a coroutine running on a
     * Vert.x event loop thread. This used to fail with "The current thread cannot be blocked" because the cached
     * value was awaited in a blocking way.
     */
    @Test
    fun testNonBlockingOnEventLoop() {
        val result = CompletableFuture<String>()
        vertx.runOnContext {
            CoroutineScope(VertxContextDispatcher(Vertx.currentContext())).launch {
                try {
                    // First call computes the value, second call is a cache hit, both on the event loop.
                    cachedService.cachedValue("event-loop-key")
                    result.complete(cachedService.cachedValue("event-loop-key"))
                } catch (e: Throwable) {
                    result.completeExceptionally(e)
                }
            }
        }
        assertThat(result.get(30, TimeUnit.SECONDS)).isEqualTo("value-event-loop-key")
    }

    class VertxContextDispatcher(private val vertxContext: Context) : CoroutineDispatcher() {
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            vertxContext.runOnContext { block.run() }
        }
    }

    @ApplicationScoped
    open class CachedService {

        private val cachedValueInvocations = AtomicInteger()
        private val defaultKeyValueInvocations = AtomicInteger()
        private val failingInvocations = AtomicInteger()

        @CacheResult(cacheName = "kotlin-suspend-cache")
        open suspend fun cachedValue(key: String): String {
            cachedValueInvocations.incrementAndGet()
            delay(10)
            // A new String instance is returned on each invocation so that cache hits can be asserted with reference
            // equality.
            return StringBuilder("value-").append(key).toString()
        }

        @CacheResult(cacheName = "kotlin-suspend-default-key-cache")
        open suspend fun defaultKeyValue(): String {
            defaultKeyValueInvocations.incrementAndGet()
            delay(10)
            return StringBuilder("default").toString()
        }

        @CacheResult(cacheName = "kotlin-suspend-composite-key-cache")
        open suspend fun compositeKeyValue(@CacheKey part1: String, @CacheKey part2: Int): String {
            delay(10)
            return StringBuilder(part1).append('-').append(part2).toString()
        }

        @CacheResult(cacheName = "kotlin-suspend-failing-cache")
        open suspend fun failing(key: String): String {
            failingInvocations.incrementAndGet()
            delay(10)
            throw IllegalStateException("expected failure")
        }

        fun cachedValueInvocations() = cachedValueInvocations.get()

        fun defaultKeyValueInvocations() = defaultKeyValueInvocations.get()

        fun failingInvocations() = failingInvocations.get()
    }
}
