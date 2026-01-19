package io.quarkus.it.vertx;

import static io.quarkus.vertx.web.Route.HttpMethod.GET;

import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.smallrye.mutiny.Uni;

@RouteBase(path = "/use-rest-client")
public class UseRestClientEndpoint {

    @Inject
    @RestClient
    SimpleRestClient call;

    @Route(path = "/greeting", methods = GET)
    public Uni<String> greeting() {
        return call.onlyGet();
    }

}
