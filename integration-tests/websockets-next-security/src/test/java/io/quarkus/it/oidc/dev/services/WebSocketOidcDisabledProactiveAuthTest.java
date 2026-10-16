package io.quarkus.it.oidc.dev.services;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@TestProfile(WebSocketOidcDisabledProactiveAuthTest.DisabledProactiveAuthProfile.class)
@QuarkusTest
public class WebSocketOidcDisabledProactiveAuthTest extends AbstractWebSocketOidcTest {

    public static class DisabledProactiveAuthProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.http.auth.proactive", "false");
        }
    }

}
