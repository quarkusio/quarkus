package io.quarkus.smallrye.reactivemessaging.runtime.kotlin

import io.quarkus.arc.Arc
import io.quarkus.arc.InjectableContext
import io.vertx.core.Context
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher

class VertxDispatcher(private val vertxContext: Context) : CoroutineDispatcher() {
    private val requestContext = Arc.container().requestContext()
    private val state: InjectableContext.ContextState
    private val destroyState: Boolean

    init {
        if (requestContext.isActive) {
            state = requestContext.state
            destroyState = false
        } else {
            destroyState = true
            state = requestContext.activate()
            requestContext.deactivate()
        }
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        vertxContext.runOnContext {
            val previousState = requestContext.stateIfActive
            requestContext.activate(state)
            try {
                block.run()
            } finally {
                if (previousState != null) {
                    requestContext.activate(previousState)
                } else {
                    requestContext.deactivate()
                }
            }
        }
    }

    fun cleanup() {
        if (destroyState) {
            requestContext.activate(state)
            requestContext.destroy()
        }
    }
}
