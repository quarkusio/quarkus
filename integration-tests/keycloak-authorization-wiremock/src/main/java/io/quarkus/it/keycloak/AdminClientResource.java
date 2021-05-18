package io.quarkus.it.keycloak;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/admin-client")
public class AdminClientResource {

    private static final Logger LOG = Logger.getLogger(AdminClientResource.class);

    @ConfigProperty(name = "keycloak.url")
    String url;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        LOG.info("Hello invoked");

        Keycloak keycloak = KeycloakBuilder.builder().serverUrl(url)
                .realm("quarkus")
                .clientId("admin-cli")
                .username("admin")
                .password("admin").build();
        return keycloak.realm("quarkus").toRepresentation().getRealm();
    }
}
