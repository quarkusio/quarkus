package io.quarkus.redis.devservices.it.profiles;

import java.util.Collections;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class DevServicesCustomPortReusableServiceProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        // This is a distinct profile, but its config should be identical to the custom port profile, so the dev service can be the same
        return Collections.singletonMap("quarkus.redis.devservices.port", DevServicesCustomPortProfile.PORT);
    }
}
