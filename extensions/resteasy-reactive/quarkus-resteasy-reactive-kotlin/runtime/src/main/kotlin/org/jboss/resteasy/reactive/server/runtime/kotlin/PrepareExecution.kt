package org.jboss.resteasy.reactive.server.runtime.kotlin

import io.vertx.core.Vertx
import jakarta.enterprise.inject.spi.CDI
import kotlinx.coroutines.CoroutineDispatcher
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext

fun prepareExecution(requestContext: ResteasyReactiveRequestContext): Pair<CoroutineDispatcher, ApplicationCoroutineScope> {
    val requestScope = requestContext.captureCDIRequestScope()
    val dispatcher: CoroutineDispatcher = Vertx.currentContext()?.let { VertxDispatcher(it, requestScope, requestContext) }
        ?: throw IllegalStateException("No Vertx context found")

    val coroutineScope = CDI.current().select(ApplicationCoroutineScope::class.java)
    requestContext.suspend()

    return Pair(dispatcher, coroutineScope.get())
}
