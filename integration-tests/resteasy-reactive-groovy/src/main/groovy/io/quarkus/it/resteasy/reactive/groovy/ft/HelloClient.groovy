package io.quarkus.it.resteasy.reactive.groovy.ft

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import org.eclipse.microprofile.faulttolerance.ExecutionContext
import org.eclipse.microprofile.faulttolerance.Fallback
import org.eclipse.microprofile.faulttolerance.FallbackHandler
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@Path("/ft/hello")
@RegisterRestClient(configKey = "ft-hello")
interface HelloClient {
    @GET @Fallback(HelloFallbackHandler.class) String hello()

    // Need to use a {@code FallbackHandler} instead of initially {@code fallbackMethod} since default methods
    // are not supported in Groovy
    class HelloFallbackHandler implements FallbackHandler<String> {

        @Override
        String handle(ExecutionContext executionContext) {
            "fallback"
        }
    }
}
