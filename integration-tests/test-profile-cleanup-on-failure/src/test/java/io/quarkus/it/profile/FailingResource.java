package io.quarkus.it.profile;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class FailingResource implements QuarkusTestResourceLifecycleManager {

    private String profile;

    @Override
    public void setContext(Context context) {
        profile = context.testProfile();
    }

    @Override
    public Map<String, String> start() {
        if (profile != null && profile.endsWith("LimitProfile")) {
            throw new IllegalStateException("simulated test resource start failure for " + profile);
        }
        return Map.of();
    }

    @Override
    public void stop() {
    }
}
