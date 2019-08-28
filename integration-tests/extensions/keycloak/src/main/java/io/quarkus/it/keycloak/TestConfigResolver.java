package io.quarkus.it.keycloak;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.representations.adapters.config.AdapterConfig;

@ApplicationScoped
public class TestConfigResolver implements KeycloakConfigResolver {
    @Override
    public KeycloakDeployment resolve(HttpFacade.Request facade) {
        String tenant = facade.getHeader("tenant");

        // if tenant-1 disables policy enforcement
        if (tenant != null && tenant.equals("tenant-1")) {
            AdapterConfig adapterConfig = new AdapterConfig();

            adapterConfig.setRealm("quarkus");
            adapterConfig.setResource("quarkus-app");
            adapterConfig.setAuthServerUrl(System.getProperty("keycloak.url", "http://localhost:8180/auth"));
            adapterConfig.setBearerOnly(true);
            Map<String, Object> credentials = new HashMap<>();

            credentials.put("secret", "secret");

            adapterConfig.setCredentials(credentials);

            return KeycloakDeploymentBuilder.build(adapterConfig);
        }

        return null;
    }
}
