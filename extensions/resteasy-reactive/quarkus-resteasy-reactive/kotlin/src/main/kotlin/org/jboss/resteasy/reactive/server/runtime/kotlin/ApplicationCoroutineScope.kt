package org.jboss.resteasy.reactive.server.runtime.kotlin

import io.vertx.core.Context
import kotlinx.coroutines.*
import org.jboss.resteasy.reactive.spi.ThreadSetupAction
import javax.annotation.PreDestroy
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Application wide coroutine scope. Should start and die with the rest of the application, along with child coroutines
 * (structured concurrency).
 *
 * This should be the main interception point to place user specific [CoroutineContext.Element] objects. Things
 * like activity ids, logger context propagation, tracing, exception handlers, etc.
 */
@Singleton
class ApplicationCoroutineScope : CoroutineScope, AutoCloseable {
    override val coroutineContext: CoroutineContext = SupervisorJob()

    @PreDestroy
    override fun close() {
        coroutineContext.cancel()
    }
}

/**
 * Dispatches the coroutine in Vertx IO thread.
 */
class VertxDispatcher(private val vertxContext: Context, private val requestScope : ThreadSetupAction.ThreadState) : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        // context propagation for suspending functions is not enabled yet, will be handled later
        vertxContext.runOnContext {
            requestScope.activate()
            try {
                block.run()
            } finally {
                requestScope.deactivate()
            }
        }
    }
}
