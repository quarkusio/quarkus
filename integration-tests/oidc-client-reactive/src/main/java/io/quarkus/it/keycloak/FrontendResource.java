package io.quarkus.it.keycloak;

import java.util.function.Function;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.mutiny.Uni;

@Path("/frontend")
public class FrontendResource {
    @Inject
    @RestClient
    ProtectedResourceServiceCustomFilter protectedResourceServiceCustomFilter;

    @Inject
    @RestClient
    ProtectedResourceServiceReactiveFilter protectedResourceServiceReactiveFilter;

    @Inject
    @RestClient
    ProtectedResourceServiceNamedFilter protectedResourceServiceNamedFilter;

    @Inject
    @RestClient
    MisconfiguredClientFilter misconfiguredClientFilter;

    @GET
    @Path("userNameCustomFilter")
    @Produces("text/plain")
    public Uni<String> userName() {
        return protectedResourceServiceCustomFilter.getUserName();
    }

    @GET
    @Path("userNameReactive")
    @Produces("text/plain")
    public Uni<String> userNameReactive() {
        return protectedResourceServiceReactiveFilter.getUserName();
    }

    @GET
    @Path("userNameNamedFilter")
    @Produces("text/plain")
    public Uni<String> userNameNamedFilter() {
        return protectedResourceServiceNamedFilter.getUserName();
    }

    @GET
    @Path("userNameMisconfiguredClientFilter")
    @Produces("text/plain")
    public Uni<String> userNameMisconfiguredClientFilter() {
        return misconfiguredClientFilter.getUserName().onFailure(Throwable.class)
                .recoverWithItem(new Function<Throwable, String>() {

                    @Override
                    public String apply(Throwable t) {
                        return t.getMessage();
                    }

                });
    }
}
