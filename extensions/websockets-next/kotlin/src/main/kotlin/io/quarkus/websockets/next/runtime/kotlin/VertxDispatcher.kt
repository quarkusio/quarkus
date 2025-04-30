package io.quarkus.websockets.next.runtime.kotlin

import io.quarkus.arc.Arc
import io.vertx.core.Context
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher

class VertxDispatcher(private val vertxContext: Context) : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        val requestContext = Arc.container().requestContext()
        vertxContext.runOnContext {
            if (requestContext.isActive) {
                block.run()
            } else {
                try {
                    requestContext.activate()
                    block.run()
                } finally {
                    requestContext.terminate()
                }
            }
        }
    }
}
