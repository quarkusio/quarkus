package io.quarkus.it.smallrye.config;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/app-config")
public class AppConfigResouce {
    @Inject
    AppConfig appConfig;

    @GET
    @Path("/name")
    public String getName() {
        return appConfig.name();
    }

    @GET
    @Path("/info/alias")
    public String getAlias() {
        return appConfig.info().alias();
    }

    @GET
    @Path("/toString")
    public String getToString() {
        return appConfig.toString();
    }
}
