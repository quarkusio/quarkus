package io.quarkus.vertx.kotlin.runtime

import io.quarkus.arc.Arc
import io.quarkus.arc.InjectableContext
import io.vertx.core.Vertx
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.concurrent.Callable
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable

@Singleton
class VertxKotlinCoroutinesDispatchers @Inject constructor(private val vertx: Vertx) {

    /**
     * Creates a [CoroutineDispatcher] that dispatches code on the Vert.x blocking context.
     *
     * Dispatches to this dispatcher will execute with context that is captured from the thread that
     * invokes `contextualBlockingDispatcher`
     */
    fun contextualBlockingDispatcher(): CoroutineDispatcher {
        val vertxContext = vertx.orCreateContext
        return VertxDispatcher { exec ->
            vertxContext.executeBlocking(Callable { exec.run() }, false)
        }
    }

    /**
     * Creates a [CoroutineDispatcher] that dispatches code on the Vert.x event loop (current
     * context if it exists).
     *
     * Dispatches to this dispatcher will execute with context that is captured from the thread that
     * invokes `contextualBlockingDispatcher`
     */
    fun contextualNonBlockingDispatcher(): CoroutineDispatcher {
        val vertxContext = vertx.orCreateContext
        return VertxDispatcher { exec -> vertxContext.runOnContext { exec.run() } }
    }

    private class VertxDispatcher(private val execution: (Runnable) -> Unit) :
        CoroutineDispatcher() {

        // capture context state at the time of dispatcher creation NOT at dispatch time
        private val requestContext = Arc.container().requestContext()
        private val state = requestContext.stateIfActive
        private val classLoader = Thread.currentThread().contextClassLoader

        fun InjectableContext.ContextState?.isNullOrInvalid(): Boolean {
            return this == null || !this.isValid
        }

        private fun replaceContext(
            nextState: InjectableContext.ContextState?,
            nextClassLoader: ClassLoader,
        ) {
            Thread.currentThread().contextClassLoader = nextClassLoader

            if (nextState.isNullOrInvalid()) {
                requestContext.deactivate()
            } else {
                requestContext.activate(nextState)
            }
        }

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            execution {
                val previousState = requestContext.stateIfActive
                val previousClassLoader = Thread.currentThread().contextClassLoader

                replaceContext(state, classLoader)
                try {
                    block.run()
                } finally {
                    replaceContext(previousState, previousClassLoader)
                }
            }
        }
    }
}
