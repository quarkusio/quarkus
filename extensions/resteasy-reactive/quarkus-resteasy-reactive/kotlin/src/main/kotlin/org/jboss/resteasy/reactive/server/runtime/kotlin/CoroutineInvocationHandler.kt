package org.jboss.resteasy.reactive.server.runtime.kotlin

import io.vertx.core.Vertx
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext
import org.jboss.resteasy.reactive.server.spi.EndpointInvoker
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(CoroutineInvocationHandler::class.java)

class CoroutineInvocationHandler(private val invoker: EndpointInvoker, private val coroutineScope: ApplicationCoroutineScope) : ServerRestHandler {
    override fun handle(requestContext: ResteasyReactiveRequestContext) {
        if (requestContext.result != null) {
            return
        }
        if (invoker !is CoroutineEndpointInvoker) {
            requestContext.handleException(IllegalStateException("Not a coroutine invoker"), true)
            return
        }

        val dispatcher: CoroutineDispatcher = Vertx.currentContext()?.let(::VertxDispatcher)
                // The @Blocking annotation will not run in a Vertx context
                ?: Dispatchers.IO

        logger.debug("Handling request with dispatcher {}", dispatcher)

        requestContext.requireCDIRequestScope()
        requestContext.suspend()
        coroutineScope.launch(context = dispatcher) {
            try {
                requestContext.result = invoker.invokeCoroutine(requestContext.endpointInstance, requestContext.parameters)
            } catch (t: Throwable) {
                // passing true since the target doesn't change and we want response filters to be able to know what the resource method was
                requestContext.handleException(t, true)
            }
            requestContext.resume()
        }
    }
}
