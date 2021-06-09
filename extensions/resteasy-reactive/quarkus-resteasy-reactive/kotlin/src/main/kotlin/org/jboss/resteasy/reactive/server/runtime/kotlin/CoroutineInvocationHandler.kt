package org.jboss.resteasy.reactive.server.runtime.kotlin

import io.vertx.core.Vertx
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext
import org.jboss.resteasy.reactive.server.spi.EndpointInvoker
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(CoroutineInvocationHandler::class.java)

class CoroutineInvocationHandler(private val invoker: EndpointInvoker,
                                 private val coroutineScope: CoroutineScope) : ServerRestHandler {

    private val originalTCCL: ClassLoader = Thread.currentThread().contextClassLoader


    override fun handle(requestContext: ResteasyReactiveRequestContext) {
        if (requestContext.result != null) {
            return
        }
        if (invoker !is CoroutineEndpointInvoker) {
            requestContext.handleException(IllegalStateException("Not a coroutine invoker"), true)
            return
        }

        val requestScope = requestContext.captureCDIRequestScope()
        val dispatcher: CoroutineDispatcher = Vertx.currentContext()?.let {VertxDispatcher(it,requestScope)}
                ?: throw IllegalStateException("No Vertx context found")

        logger.trace("Handling request with dispatcher {}", dispatcher)

        requestContext.suspend()
        coroutineScope.launch(context = dispatcher) {
            // ensure the proper CL is not lost in dev-mode
            Thread.currentThread().contextClassLoader = originalTCCL
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
