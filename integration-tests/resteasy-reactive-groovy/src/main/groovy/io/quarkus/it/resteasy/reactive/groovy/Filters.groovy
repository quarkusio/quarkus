package io.quarkus.it.resteasy.reactive.groovy

import groovy.transform.CompileStatic
import io.smallrye.mutiny.Uni
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ResourceInfo
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriInfo
import org.jboss.resteasy.reactive.server.ServerRequestFilter
import org.jboss.resteasy.reactive.server.ServerResponseFilter
import org.jboss.resteasy.reactive.server.SimpleResourceInfo

import java.security.SecureRandom
import java.time.Duration
import java.util.function.Function

@CompileStatic
class Filters {

    private SecureRandom secureRandom = new SecureRandom()

    @ServerRequestFilter
    Uni<Void> addHeader(UriInfo uriInfo, ContainerRequestContext context) {
        Uni.createFrom().nullItem()
                .onItem().delayIt().by(Duration.ofMillis(100))
                .onItem().transform({ context.headers.add("firstName", "foo") } as Function)
                .onItem().delayIt().by(Duration.ofMillis(100))
                .replaceWithVoid()
    }

    @ServerRequestFilter
    Uni<Response> addHeaderOrAbort(ContainerRequestContext context)  {
        Uni.createFrom().nullItem()
            .onItem().delayIt().by(Duration.ofMillis(100))
            .onItem().transform(
                {
                    if (context.headers.containsKey("abort")) {
                        return Response.noContent().header("random", "" + secureRandom.nextInt()).build()
                    }
                    context.headers.add("lastName", "bar")
                    null
                }
            )
            .onItem().delayIt().by(Duration.ofMillis(100))
    }

    @ServerResponseFilter
    Uni<Void> addResponseHeader(
            ContainerResponseContext context,
            SimpleResourceInfo simpleResourceInfo,
            ResourceInfo resourceInfo
    ) {
        Uni.createFrom().nullItem()
            .onItem().delayIt().by(Duration.ofMillis(100))
            .onItem().invoke {
                context.headers.add("method", simpleResourceInfo.methodName)
                context.headers.add("method2", resourceInfo.resourceMethod.name)
            }
            .onItem().delayIt().by(Duration.ofMillis(100))
            .replaceWithVoid()
    }
}
