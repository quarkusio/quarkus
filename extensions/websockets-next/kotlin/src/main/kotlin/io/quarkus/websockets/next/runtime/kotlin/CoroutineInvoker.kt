package io.quarkus.websockets.next.runtime.kotlin

import io.quarkus.arc.Arc
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import io.vertx.core.Vertx
import jakarta.enterprise.invoke.Invoker
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async

object CoroutineInvoker {
    @JvmStatic
    @OptIn(ExperimentalCoroutinesApi::class)
    fun <T, U> inNewCoroutine(instance: T, arguments: Array<Any?>, invoker: Invoker<T, U>): Uni<U> {
        val coroutineScope = Arc.container().instance(ApplicationCoroutineScope::class.java).get()
        val dispatcher: CoroutineDispatcher =
            Vertx.currentContext()?.let(::VertxDispatcher)
                ?: throw IllegalStateException("No Vertx context found")

        return coroutineScope
            .async<U>(context = dispatcher) {
                suspendCoroutine { continuation ->
                    arguments[arguments.size - 1] = continuation
                    try {
                        continuation.resume(invoker.invoke(instance, arguments))
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }
            }
            .asUni()
    }
}
