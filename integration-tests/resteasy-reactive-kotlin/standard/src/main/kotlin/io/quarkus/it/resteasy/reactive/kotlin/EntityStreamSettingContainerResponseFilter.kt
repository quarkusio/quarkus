package io.quarkus.it.resteasy.reactive.kotlin

import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.ext.Provider
import java.io.ByteArrayInputStream
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext

@Provider
class EntityStreamSettingContainerResponseFilter : ContainerResponseFilter {
    override fun filter(
        requestContext: ContainerRequestContext?,
        responseContext: ContainerResponseContext?
    ) {
        if (requestContext is ResteasyReactiveContainerRequestContext) {
            if (
                "hello".equals(
                    requestContext.serverRequestContext.resteasyReactiveResourceInfo.name
                )
            ) {
                responseContext?.setEntity(
                    ByteArrayInputStream("Hello Quarkus REST".toByteArray(Charsets.UTF_8))
                )
            }
        }
    }
}
