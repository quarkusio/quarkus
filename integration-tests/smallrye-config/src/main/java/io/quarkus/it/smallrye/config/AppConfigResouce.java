package io.quarkus.it.smallrye.config;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
}
