package org.jboss.resteasy.reactive.server.runtime.kotlin

import jakarta.ws.rs.container.ContainerResponseContext
import kotlinx.coroutines.launch
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerResponseFilter

/**
 * Base class used by Quarkus to generate an implementation at build-time that calls a `suspend`
 * method annotated with
 * [org.jboss.resteasy.reactive.server.ServerResponseFilter](ServerResponseFilter)
 */
@Suppress("unused")
abstract class AbstractSuspendedResponseFilter : ResteasyReactiveContainerResponseFilter {

    abstract suspend fun doFilter(
        requestContext: ResteasyReactiveContainerRequestContext,
        responseContext: ContainerResponseContext
    ): Any

    private val originalTCCL: ClassLoader = Thread.currentThread().contextClassLoader

    override fun filter(
        requestContext: ResteasyReactiveContainerRequestContext,
        responseContext: ContainerResponseContext
    ) {
        val (dispatcher, coroutineScope) =
            prepareExecution(requestContext.serverRequestContext as ResteasyReactiveRequestContext)

        requestContext.suspend()
        coroutineScope.launch(context = dispatcher) {
            // ensure the proper CL is not lost in dev-mode
            Thread.currentThread().contextClassLoader = originalTCCL
            try {
                doFilter(requestContext, responseContext)
            } catch (t: Throwable) {
                (requestContext.serverRequestContext as ResteasyReactiveRequestContext)
                    .handleException(t, true)
            }
            requestContext.resume()
        }
    }
}
