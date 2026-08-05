package io.quarkus.vertx.kotlin.runtime

import io.quarkus.arc.Arc
import io.quarkus.arc.Unremovable
import io.vertx.core.Vertx
import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

@Singleton
@Unremovable
class ContextPreservingCoroutineDispatcher : CoroutineDispatcher() {

    private val capturedContexts = ConcurrentHashMap<Job, io.vertx.core.Context>()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        val job = context[Job]
        val currentVtxCtx = Vertx.currentContext()

        if (
            currentVtxCtx != null &&
                job != null &&
                capturedContexts.putIfAbsent(job, currentVtxCtx) == null
        ) {
            job.invokeOnCompletion { capturedContexts.remove(job) }
        }

        val vtxCtx = (job?.let { capturedContexts[it] } ?: currentVtxCtx)

        if (vtxCtx != null) {
            val requestContext = Arc.container().requestContext()
            vtxCtx.runOnContext {
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
        } else {
            Dispatchers.Default.dispatch(context, block)
        }
    }
}
