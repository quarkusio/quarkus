package io.quarkus.keycloak.admin.client.reactive.runtime;

import org.keycloak.admin.client.Keycloak;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ResteasyReactiveKeycloakAdminClientRecorder {

    public void setClientProvider() {
        Keycloak.setClientProvider(new ResteasyReactiveClientProvider());
    }
}
