package io.quarkus.vertx.http.runtime;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.runtime.LaunchMode;

/**
 * Sets the default host config value, depending on the launch mode.
 * <p>
 * This can't be done with a normal default, is it changes based on the mode,
 * so instead it is provided as a low priority config source.
 */
public class HttpHostConfigSource implements ConfigSource, Serializable {

    public static final String QUARKUS_HTTP_HOST = "quarkus.http.host";
    private static final String ALL_INTERFACES = "0.0.0.0";

    private final int priority;

    public HttpHostConfigSource(int priority) {
        this.priority = priority;
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.singletonMap(QUARKUS_HTTP_HOST, getValue(QUARKUS_HTTP_HOST));
    }

    @Override
    public Set<String> getPropertyNames() {
        return Collections.singleton(QUARKUS_HTTP_HOST);
    }

    @Override
    public int getOrdinal() {
        return priority;
    }

    @Override
    public String getValue(String propertyName) {
        if (propertyName.equals(QUARKUS_HTTP_HOST)) {
            if (LaunchMode.isRemoteDev()) {
                // in remote-dev mode we need to listen on all interfaces
                return ALL_INTERFACES;
            }
            return LaunchMode.current().isDevOrTest() ? "localhost" : ALL_INTERFACES;
        }
        return null;
    }

    @Override
    public String getName() {
        return "Quarkus HTTP Host Default Value";
    }
}
