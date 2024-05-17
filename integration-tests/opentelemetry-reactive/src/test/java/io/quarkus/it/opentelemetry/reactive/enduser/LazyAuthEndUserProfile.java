package io.quarkus.it.opentelemetry.reactive.enduser;

import java.util.HashMap;
import java.util.Map;

public class LazyAuthEndUserProfile extends EndUserProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        var config = new HashMap<>(super.getConfigOverrides());
        config.put("quarkus.http.auth.proactive", "false");
        return config;
    }
}
