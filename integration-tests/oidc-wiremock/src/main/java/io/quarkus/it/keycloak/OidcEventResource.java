package io.quarkus.it.keycloak;

import static io.quarkus.it.keycloak.OidcEventObserver.dropTrailingSlash;

import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.oidc.runtime.OidcConfig;

@Path("/oidc-event")
public class OidcEventResource {

    private final OidcEventObserver oidcEventObserver;
    private final String expectedAuthServerUrl;

    public OidcEventResource(OidcEventObserver oidcEventObserver, OidcConfig oidcConfig) {
        this.expectedAuthServerUrl = dropTrailingSlash(OidcConfig.getDefaultTenant(oidcConfig).authServerUrl().get());
        this.oidcEventObserver = oidcEventObserver;
    }

    @Path("/unavailable-auth-server-urls")
    @GET
    public String unavailableAuthServerUrls() {
        return oidcEventObserver
                .getUnavailableAuthServerUrls()
                .stream()
                .sorted(String::compareTo)
                .collect(Collectors.joining("-"));
    }

    @Path("/available-auth-server-urls")
    @GET
    public String availableAuthServerUrls() {
        return oidcEventObserver
                .getAvailableAuthServerUrls()
                .stream()
                .sorted(String::compareTo)
                .collect(Collectors.joining("-"));
    }

    @GET
    @Path("/expected-auth-server-url")
    public String getExpectedAuthServerUrl() {
        return expectedAuthServerUrl;
    }

}
