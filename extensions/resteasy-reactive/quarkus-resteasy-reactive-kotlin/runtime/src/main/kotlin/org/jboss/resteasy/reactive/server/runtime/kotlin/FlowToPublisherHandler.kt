package org.jboss.resteasy.reactive.server.runtime.kotlin

import io.smallrye.mutiny.coroutines.asMulti
import io.vertx.core.Vertx
import jakarta.enterprise.inject.spi.CDI
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler

class FlowToPublisherHandler : ServerRestHandler {

    private val originalTCCL: ClassLoader = Thread.currentThread().contextClassLoader

    override fun handle(requestContext: ResteasyReactiveRequestContext?) {
        val result = requestContext!!.result
        if (result is Flow<*>) {
            val requestScope = requestContext.captureCDIRequestScope()
            val dispatcher: CoroutineDispatcher =
                Vertx.currentContext()?.let { VertxDispatcher(it, requestScope, requestContext) }
                    ?: throw IllegalStateException("No Vertx context found")

            val coroutineScope = CDI.current().select(ApplicationCoroutineScope::class.java)
            requestContext.suspend()
            coroutineScope.get().launch(context = dispatcher) {
                // ensure the proper CL is not lost in dev-mode
                Thread.currentThread().contextClassLoader = originalTCCL
                requestContext.result = result.asMulti()
                // run in a direct invocation executor to run the rest of the invocation in the
                // co-route scope
                // feels a bit fragile, but let's see how it goes
                requestContext.resume(Executor { it.run() })
            }
        }
    }
}
