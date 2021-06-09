package org.jboss.resteasy.reactive.server.runtime.kotlin

import io.quarkus.arc.Unremovable
import org.jboss.resteasy.reactive.server.spi.EndpointInvoker
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory for the [CoroutineInvocationHandler] that is already part of the CDI container
 */
@Singleton
@Unremovable
class CoroutineInvocationHandlerFactory(private val applicationCoroutineScope: ApplicationCoroutineScope) {
    fun createHandler(invoker: EndpointInvoker): ServerRestHandler {
        return CoroutineInvocationHandler(invoker, applicationCoroutineScope)
    }
}
