package org.jboss.resteasy.reactive.server.runtime.kotlin

import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer
import org.jboss.resteasy.reactive.server.model.ServerResourceMethod
import org.jboss.resteasy.reactive.server.spi.EndpointInvoker
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler
import java.util.function.Supplier

open class CoroutineMethodProcessor (var supplier: Supplier<EndpointInvoker>? = null) : HandlerChainCustomizer {
    override fun alternateInvocationHandler(invoker: EndpointInvoker): ServerRestHandler {
        return CoroutineInvocationHandler(invoker)
    }

    override fun alternateInvoker(method: ServerResourceMethod): Supplier<EndpointInvoker>? {
        return supplier
    }
}
