package io.quarkus.runtime.configuration;

import java.util.Set;
import java.util.UUID;

import jakarta.annotation.Priority;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.Priorities;

@Priority(Priorities.LIBRARY)
public class QuarkusUUIDConfigSource implements ConfigSource {

    static final String QUARKUS_UUID_CONFIG_KEY = "quarkus.uuid";

    public static final QuarkusUUIDConfigSource INSTANCE = new QuarkusUUIDConfigSource();

    private volatile String cachedQuarkusUUID;

    @Override
    public Set<String> getPropertyNames() {
        return Set.of(QUARKUS_UUID_CONFIG_KEY);
    }

    @Override
    public String getValue(String propertyName) {
        if (!QUARKUS_UUID_CONFIG_KEY.equals(propertyName)) {
            return null;
        }

        String localQuarkusUUID = cachedQuarkusUUID;

        if (localQuarkusUUID != null) {
            return localQuarkusUUID;
        }

        synchronized (this) {
            localQuarkusUUID = cachedQuarkusUUID;

            if (localQuarkusUUID != null) {
                return localQuarkusUUID;
            }

            return cachedQuarkusUUID = UUID.randomUUID().toString();
        }
    }

    @Override
    public String getName() {
        return QuarkusUUIDConfigSource.class.getName();
    }
}
