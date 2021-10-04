package org.jboss.resteasy.reactive.server.runtime.kotlin

import org.jboss.resteasy.reactive.server.spi.EndpointInvoker

/**
 * Base interface implemented by the synthetic beans that represent suspending rest endpoints.
 *
 * @see [EndpointInvoker]
 *
 * Also see {@code io.quarkus.resteasy.reactive.kotlin.deployment.KotlinCoroutineIntegrationProcessor} for the build-time part
 * of coroutine support
 */
interface CoroutineEndpointInvoker: EndpointInvoker {
    /**
     * Delegates control over the bean that defines the endpoint
     *
     * **API note:** Actual Java synthetic bytecode has a 3rd parameter that is the actual coroutine state machine.
     * @param instance the rest endpoint instance bean
     * @param parameters the call parameters
     * @return the coroutine result which may be the computation result, or a suspension point.
     */
    suspend fun invokeCoroutine(instance: Any, parameters: Array<out Any>): Any?
}
