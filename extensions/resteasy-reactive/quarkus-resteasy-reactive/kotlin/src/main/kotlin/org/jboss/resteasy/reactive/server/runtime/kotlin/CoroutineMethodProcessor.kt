package org.jboss.resteasy.reactive.server.runtime.kotlin

import io.quarkus.arc.Arc
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer
import org.jboss.resteasy.reactive.server.model.ServerResourceMethod
import org.jboss.resteasy.reactive.server.spi.EndpointInvoker
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler
import java.util.function.Supplier

/**
 * Intercepts method invocations to force an EndpointInvoker.
 */
//this class is serialized, hence the default value
open class CoroutineMethodProcessor @Deprecated("Used only in synthetic code") constructor() : HandlerChainCustomizer {

    constructor(alternativeInvoker: Supplier<EndpointInvoker>): this() {
        this.alternativeInvoker = alternativeInvoker
    }

    private lateinit var alternativeInvoker: Supplier<EndpointInvoker>

    private val scope by lazy { Arc.container().instance(ApplicationCoroutineScope::class.java).get() }

    override fun alternateInvocationHandler(invoker: EndpointInvoker): ServerRestHandler {
        return CoroutineInvocationHandler(invoker, scope)
    }

    override fun alternateInvoker(method: ServerResourceMethod): Supplier<EndpointInvoker>? {
        return alternativeInvoker
    }
}
