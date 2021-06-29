package org.jboss.resteasy.reactive.server.runtime.kotlin

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asPublisher
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler

class FlowToPublisherHandler : ServerRestHandler {

    override fun handle(requestContext: ResteasyReactiveRequestContext?) {
        val result = requestContext!!.result
        if (result is Flow<*>) {
            requestContext.result = (result as Flow<Any>) // cast needed for extension function
                                        .asPublisher()
        }
    }
}
