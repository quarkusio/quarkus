package io.quarkus.it.keycloak;

import static io.quarkus.oidc.SecurityEvent.AUTH_SERVER_URL;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import io.quarkus.oidc.SecurityEvent;

@Singleton
public class OidcEventObserver {

    private final Set<String> unavailableAuthServerUrls = ConcurrentHashMap.newKeySet();
    private final Set<String> availableAuthServerUrls = ConcurrentHashMap.newKeySet();

    void observe(@Observes SecurityEvent event) {
        if (event.getEventType() == SecurityEvent.Type.OIDC_SERVER_NOT_AVAILABLE) {
            String authServerUrl = dropTrailingSlash((String) event.getEventProperties().get(AUTH_SERVER_URL));
            unavailableAuthServerUrls.add(authServerUrl);
        } else if (event.getEventType() == SecurityEvent.Type.OIDC_SERVER_AVAILABLE) {
            String authServerUrl = (String) event.getEventProperties().get(AUTH_SERVER_URL);
            availableAuthServerUrls.add(authServerUrl);
        }
    }

    public static String dropTrailingSlash(String authServerUrl) {
        if (authServerUrl.endsWith("/")) {
            // drop ending slash as these auth server urls are same in principle
            return authServerUrl.substring(0, authServerUrl.length() - 1);
        }
        return authServerUrl;
    }

    Set<String> getUnavailableAuthServerUrls() {
        return unavailableAuthServerUrls;
    }

    Set<String> getAvailableAuthServerUrls() {
        return availableAuthServerUrls;
    }
}
