package io.quarkus.it.vertx;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.rest.client.reactive.VertxRequestClientHeadersFactory;
import io.smallrye.mutiny.Uni;

@RegisterRestClient(configKey = "simple")
@RegisterClientHeaders(VertxRequestClientHeadersFactory.class)
public interface SimpleRestClient {

    @GET
    @Path("/only-get")
    Uni<String> onlyGet();
}
