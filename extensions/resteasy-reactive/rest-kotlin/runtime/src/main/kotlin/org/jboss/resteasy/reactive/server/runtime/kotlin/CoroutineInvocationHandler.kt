package org.jboss.resteasy.reactive.server.runtime.kotlin

import io.vertx.core.Vertx
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext
import org.jboss.resteasy.reactive.server.spi.EndpointInvoker
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(CoroutineInvocationHandler::class.java)

class CoroutineInvocationHandler(
    private val invoker: EndpointInvoker,
    private val coroutineScope: CoroutineScope,
) : ServerRestHandler {

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
        val dispatcher: CoroutineDispatcher =
            Vertx.currentContext()?.let { VertxDispatcher(it, requestScope, requestContext) }
                ?: throw IllegalStateException("No Vertx context found")

        logger.trace("Handling request with dispatcher {}", dispatcher)

        requestContext.suspend()
        val done = AtomicBoolean()
        val canceled = AtomicBoolean()

        val job =
            coroutineScope.launch(context = dispatcher) {
                // ensure the proper CL is not lost in dev-mode
                Thread.currentThread().contextClassLoader = originalTCCL
                try {
                    val result =
                        invoker.invokeCoroutine(
                            requestContext.endpointInstance,
                            requestContext.parameters,
                        )
                    done.set(true)
                    if (result != Unit) {
                        requestContext.result = result
                    }
                    requestContext.resume()
                } catch (t: Throwable) {
                    done.set(true)

                    if (canceled.get()) {
                        try {
                            // get rid of everything related to the request since the connection has
                            // already gone away
                            requestContext.close()
                        } catch (ignored: Exception) {}
                    } else {
                        // passing true since the target doesn't change and we want response filters
                        // to
                        // be able to know what the resource method was
                        requestContext.handleException(t, true)
                        requestContext.resume()
                    }
                }
            }

        requestContext.serverResponse().addCloseHandler {
            if (!done.get()) {
                try {
                    canceled.set(true)
                    job.cancel(null)
                } catch (ignored: Exception) {}
            }
        }
    }
}
