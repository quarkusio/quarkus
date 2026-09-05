package io.quarkus.cache.kotlin.test

import io.quarkus.cache.CacheResult
import io.quarkus.test.QuarkusExtensionTest
import io.restassured.RestAssured
import io.smallrye.common.vertx.ContextLocals
import io.smallrye.common.vertx.VertxContext
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * Verifies that Vert.x context locals and CDI request-scoped state remain available across
 * `@CacheResult` interception of Kotlin suspend functions (cache miss and cache hit), when the
 * cached suspend method is invoked from a REST endpoint that starts the request context.
 */
class KotlinSuspendCacheContextPropagationTest {

    companion object {
        const val LOCAL_KEY = "cache-kotlin-context-marker"

        @RegisterExtension
        val test = QuarkusExtensionTest()
            .withApplicationRoot { jar ->
                jar.addClasses(
                    CacheContextResource::class.java,
                    CachedService::class.java,
                    RequestData::class.java
                )
            }
    }

    @Inject
    lateinit var cachedService: CachedService

    @BeforeEach
    fun reset() {
        cachedService.reset()
    }

    @Test
    fun testContextLocalsAndRequestScopeOnCacheMissAndHit() {
        // Cache miss: REST starts the request context, then invokes the cached suspend method.
        RestAssured.`when`().get("/cache-context/key-1")
            .then().statusCode(200).body(`is`("value-key-1"))

        assertThat(cachedService.invocations()).isEqualTo(1)
        assertThat(cachedService.localBeforeDelay.get()).isEqualTo("local-value")
        assertThat(cachedService.localAfterDelay.get()).isEqualTo("local-value")
        assertThat(cachedService.requestBeforeDelay.get()).isEqualTo("request-value")
        assertThat(cachedService.requestAfterDelay.get()).isEqualTo("request-value")

        // Cache hit: method body is skipped, but contexts must still be available after the call.
        RestAssured.`when`().get("/cache-context/key-1")
            .then().statusCode(200).body(`is`("value-key-1"))

        assertThat(cachedService.invocations()).isEqualTo(1)
    }

    @Path("/cache-context")
    class CacheContextResource {

        @Inject
        lateinit var cachedService: CachedService

        @Inject
        lateinit var requestData: RequestData

        @GET
        @Path("/{key}")
        suspend fun get(@PathParam("key") key: String): String {
            ContextLocals.put(LOCAL_KEY, "local-value")
            requestData.value = "request-value"

            val value = cachedService.cachedValue(key)

            // Context must still be accessible after the cached suspend call returns.
            assertThat(ContextLocals.get(LOCAL_KEY, null as String?)).isEqualTo("local-value")
            assertThat(requestData.value).isEqualTo("request-value")
            assertThat(VertxContext.isOnDuplicatedContext()).isTrue()

            return value
        }
    }

    @RequestScoped
    class RequestData {
        var value: String = ""
    }

    @ApplicationScoped
    open class CachedService {

        @Inject
        lateinit var requestData: RequestData

        private val invocations = AtomicInteger()
        val localBeforeDelay = AtomicReference<String>()
        val localAfterDelay = AtomicReference<String>()
        val requestBeforeDelay = AtomicReference<String>()
        val requestAfterDelay = AtomicReference<String>()

        @CacheResult(cacheName = "kotlin-suspend-context-cache")
        open suspend fun cachedValue(key: String): String {
            invocations.incrementAndGet()
            assertThat(VertxContext.isOnDuplicatedContext()).isTrue()
            localBeforeDelay.set(ContextLocals.get(LOCAL_KEY, null as String?))
            requestBeforeDelay.set(requestData.value)
            delay(10)
            assertThat(VertxContext.isOnDuplicatedContext()).isTrue()
            localAfterDelay.set(ContextLocals.get(LOCAL_KEY, null as String?))
            requestAfterDelay.set(requestData.value)
            return StringBuilder("value-").append(key).toString()
        }

        fun invocations() = invocations.get()

        fun reset() {
            invocations.set(0)
            localBeforeDelay.set(null)
            localAfterDelay.set(null)
            requestBeforeDelay.set(null)
            requestAfterDelay.set(null)
        }
    }
}
