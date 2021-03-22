package org.jboss.resteasy.reactive.server.runtime.kotlin

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext
import org.jboss.resteasy.reactive.server.spi.EndpointInvoker
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler

class CoroutineInvocationHandler(private val invoker: EndpointInvoker) : ServerRestHandler {
    override fun handle(requestContext: ResteasyReactiveRequestContext) {
        if (requestContext.result != null) {
            return
        }
        requestContext.requireCDIRequestScope()
        requestContext.suspend()
        GlobalScope.launch {
            try {
                if (invoker is CoroutineEndpointInvoker) {
                    requestContext.result = invoker.invokeCoroutine(requestContext.endpointInstance, requestContext.parameters)
                } else {
                    throw Exception("Not a CoroutineEndpointInvoker")
                }
            } catch (t: Throwable) {
                // passing true since the target doesn't change and we want response filters to be able to know what the resource method was
                requestContext.handleException(t, true)
            }
            requestContext.resume()
        }
    }
}
