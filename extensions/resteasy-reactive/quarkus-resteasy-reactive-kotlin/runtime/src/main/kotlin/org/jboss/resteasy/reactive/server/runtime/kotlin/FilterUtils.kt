package org.jboss.resteasy.reactive.server.runtime.kotlin

import io.vertx.core.Vertx
import kotlinx.coroutines.CoroutineDispatcher
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext
import javax.enterprise.inject.spi.CDI

fun prepareExecution(requestContext: ResteasyReactiveRequestContext): Pair<CoroutineDispatcher, ApplicationCoroutineScope> {
    val requestScope = requestContext.captureCDIRequestScope()
    val dispatcher: CoroutineDispatcher = Vertx.currentContext()?.let {VertxDispatcher(it,requestScope)}
            ?: throw IllegalStateException("No Vertx context found")

    val coroutineScope = CDI.current().select(ApplicationCoroutineScope::class.java)
    requestContext.suspend()

    return Pair(dispatcher, coroutineScope.get())
}
