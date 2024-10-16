package io.quarkus.it.keycloak;

import java.util.Map;
import java.util.Set;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@PermitAll
@Path("startup-service")
public class StartupResource {

    private final StartupService startupService;

    public StartupResource(StartupService startupService) {
        this.startupService = startupService;
    }

    @GET
    public Map<String, Map<String, Set<String>>> tenantToIdentityWithRole() {
        return startupService.getTenantToIdentityWithRole();
    }

}
