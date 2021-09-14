package io.quarkus.smallrye.reactivemessaging.runtime.kotlin

import io.quarkus.arc.Arc
import io.vertx.core.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.coroutines.CoroutineContext

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
