package io.quarkus.oidc.token.propagation.graphql.deployment.test;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/dev-mode-frontend")
public class DevModeFrontendResource {

    @Inject
    DevModeTypesafeGraphQLClient client;

    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String authServerUrl;

    @GET
    @Path("/me")
    public String me() {
        return client.me();
    }

    @GET
    @Path("/auth-server-url")
    public String authServerUrl() {
        return authServerUrl;
    }
}
