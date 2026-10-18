package io.quarkus.it.oidc;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.util.JsonSerialization;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.quarkus.test.keycloak.client.KeycloakTestClient.Tls;

public class KeycloakTestResourceLifecycleManager implements QuarkusTestResourceLifecycleManager,
        DevServicesContext.ContextAware {

    KeycloakTestClient client = new KeycloakTestClient(
            new Tls("target/certificates/oidc-client-keystore.p12",
                    "target/certificates/oidc-client-truststore.p12"));

    @Override
    public Map<String, String> start() {

        client.createRealm(loadRealm());

        return Map.of();
    }

    private static RealmRepresentation loadRealm() {
        try {
            URL realmPathUrl = Thread.currentThread().getContextClassLoader().getResource("quarkus-realm.json");
            try (InputStream is = realmPathUrl.openStream()) {
                return JsonSerialization.readValue(is, RealmRepresentation.class);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        client.setIntegrationTestContext(context);
    }

    @Override
    public void stop() {
    }
}
