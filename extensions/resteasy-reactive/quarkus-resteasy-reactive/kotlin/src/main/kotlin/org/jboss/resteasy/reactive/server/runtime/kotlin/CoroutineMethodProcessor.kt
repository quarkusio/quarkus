package org.jboss.resteasy.reactive.server.runtime.kotlin

import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer
import org.jboss.resteasy.reactive.server.model.ServerResourceMethod
import org.jboss.resteasy.reactive.server.spi.EndpointInvoker
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler
import java.util.function.Supplier
import javax.enterprise.inject.spi.CDI

/**
 * Intercepts method invocations to force an EndpointInvoker.
 */
open class CoroutineMethodProcessor @Deprecated("Used only in synthetic code") constructor() : HandlerChainCustomizer {

    constructor(alternativeInvoker: Supplier<EndpointInvoker>): this() {
        this.alternativeInvoker = alternativeInvoker
    }

    lateinit var alternativeInvoker: Supplier<EndpointInvoker>

    // not pretty, but this seems to be a limitation of the current method scanning process
    // the HandlerChainCustomizer is called in a build step, but also at runtime to actually create the invocation handler
    // classes like SecurityContextOverrideHandler also use this approach
    private val handlerFactory by lazy { CDI.current().select(CoroutineInvocationHandlerFactory::class.java).get() }

    override fun alternateInvocationHandler(invoker: EndpointInvoker): ServerRestHandler {
        return handlerFactory.createHandler(invoker)
    }

    override fun alternateInvoker(method: ServerResourceMethod): Supplier<EndpointInvoker>? {
        return alternativeInvoker
    }
}
