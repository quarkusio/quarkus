package io.quarkus.it.resteasy.reactive.kotlin

import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriInfo
import kotlinx.coroutines.delay
import org.jboss.resteasy.reactive.server.ServerRequestFilter
import org.jboss.resteasy.reactive.server.ServerResponseFilter
import org.jboss.resteasy.reactive.server.SimpleResourceInfo

class Filters {

    @ServerRequestFilter
    suspend fun addHeader(uriInfo: UriInfo, context: ContainerRequestContext) {
        delay(100)
        context.headers.add("firstName", "foo")
        delay(100)
    }

    @ServerRequestFilter
    suspend fun addHeaderOrAbort(context: ContainerRequestContext): Response? {
        delay(100)
        if (context.headers.containsKey("abort")) {
            return Response.noContent().build()
        }
        context.headers.add("lastName", "bar")
        delay(100)
        return null
    }

    @ServerResponseFilter
    suspend fun addResponseHeader(context: ContainerResponseContext, simpleResourceInfo: SimpleResourceInfo) {
        delay(100)
        context.headers.add("method", simpleResourceInfo.methodName)
        delay(100)
    }
}
