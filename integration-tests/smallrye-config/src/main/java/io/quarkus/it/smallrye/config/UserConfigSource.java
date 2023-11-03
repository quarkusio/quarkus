package io.quarkus.it.smallrye.config;

import java.util.HashMap;
import java.util.Map;

import io.smallrye.config.common.MapBackedConfigSource;

public class UserConfigSource extends MapBackedConfigSource {
    private static final Map<String, String> USER_CONFIG = new HashMap<>();

    static {
        USER_CONFIG.put("user.config.prop", "1234");
    }

    public UserConfigSource() {
        this(USER_CONFIG);
    }

    public UserConfigSource(final Map<String, String> properties) {
        super(UserConfigSource.class.getSimpleName(), properties);
    }
}
