package org.jboss.resteasy.reactive.server.runtime.kotlin

import io.vertx.core.Context
import kotlinx.coroutines.*
import org.eclipse.microprofile.context.ManagedExecutor
import javax.annotation.PreDestroy
import javax.enterprise.context.ApplicationScoped
import kotlin.coroutines.CoroutineContext

/**
 * Application wide coroutine scope. Should start and die with the rest of the application, along with child coroutines
 * (structured concurrency).
 *
 * This should be the main interception point to place user specific [CoroutineContext.Element] objects. Things
 * like activity ids, logger context propagation, tracing, exception handlers, etc.
 */
@ApplicationScoped
class ApplicationCoroutineScope : CoroutineScope, AutoCloseable {
    override val coroutineContext: CoroutineContext = SupervisorJob()

    @PreDestroy
    override fun close() {
        coroutineContext.cancel()
    }
}

/**
 * Dispatches the coroutine in a worker thread from Vertx.
 */
class VertxDispatcher(private val vertxContext: Context) : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        vertxContext.runOnContext {
            block.run()
        }
    }
}

/**
 * Dispatches coroutine into the managed thread pool
 */
class ExecutorDispatcher(private val managedExecutor: ManagedExecutor): CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        // todo may need to handle failed job submissions
        managedExecutor.execute(block)
    }
}