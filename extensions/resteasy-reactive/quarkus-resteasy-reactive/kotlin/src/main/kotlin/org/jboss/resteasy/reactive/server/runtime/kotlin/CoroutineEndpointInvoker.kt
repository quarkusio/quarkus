package org.jboss.resteasy.reactive.server.runtime.kotlin

import org.jboss.resteasy.reactive.server.spi.EndpointInvoker

interface CoroutineEndpointInvoker: EndpointInvoker {
    suspend fun invokeCoroutine(instance: Any, parameters: Array<out Any>): Any?
}
