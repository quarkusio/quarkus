package io.quarkus.scheduler.kotlin.runtime

import io.quarkus.arc.Arc
import io.quarkus.scheduler.ScheduledExecution
import io.quarkus.scheduler.common.runtime.ScheduledInvoker
import io.vertx.core.Vertx
import java.util.concurrent.CompletionStage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture

abstract class AbstractCoroutineInvoker : ScheduledInvoker {

    override fun invoke(execution: ScheduledExecution): CompletionStage<Void> {
        val coroutineScope = Arc.container().instance(ApplicationCoroutineScope::class.java).get()
        val dispatcher: CoroutineDispatcher =
            Vertx.currentContext()?.let(::VertxDispatcher)
                ?: throw IllegalStateException("No Vertx context found")

        return coroutineScope
            .async(context = dispatcher) { invokeBean(execution) }
            .asCompletableFuture()
    }

    abstract suspend fun invokeBean(execution: ScheduledExecution): Void
}
