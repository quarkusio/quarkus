package io.quarkus.keycloak.admin.resteasy.client.runtime;

import org.keycloak.admin.client.Keycloak;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.keycloak.admin.client.common.runtime.KeycloakAdminClientConfig;

public class KeycloakAdminClientCreator implements BeanCreator<Keycloak> {

    @Override
    public Keycloak create(SyntheticCreationalContext<Keycloak> context) {
        return KeycloakAdminResteasyClientRecorder
                .createAdminClient(context.getInjectedReference(KeycloakAdminClientConfig.class));
    }
}
