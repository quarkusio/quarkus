package org.jboss.resteasy.reactive.server.runtime.kotlin

import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import kotlinx.coroutines.async
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestFilter

/**
 * Base class used by Quarkus to generate an implementation at build-time that calls
 * a {@code suspend} method annotated with {@code @ServerRequestFilter}
 */
@Suppress("unused")
abstract class AbstractSuspendedRequestFilter : ResteasyReactiveContainerRequestFilter {

    abstract suspend fun doFilter(containerRequestContext: ResteasyReactiveContainerRequestContext): Any

    abstract fun handleResult(containerRequestContext: ResteasyReactiveContainerRequestContext, uniResult: Uni<*>)


    private val originalTCCL: ClassLoader = Thread.currentThread().contextClassLoader

    override fun filter(containerRequestContext: ResteasyReactiveContainerRequestContext) {
        val (dispatcher,coroutineScope) = prepareExecution(containerRequestContext.serverRequestContext as ResteasyReactiveRequestContext)

        val uni = coroutineScope.async(context = dispatcher) {
            // ensure the proper CL is not lost in dev-mode
            Thread.currentThread().contextClassLoader = originalTCCL

            // the implementation gets the proper values from the context and invokes the user supplied method
            doFilter(containerRequestContext)
        }.asUni()

        // the implementation should call the appropriate FilterUtil method
        handleResult(containerRequestContext, uni)
    }
}
