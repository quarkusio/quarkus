package io.quarkus.vertx.http.runtime;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

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

    private final int priority;

    public HttpHostConfigSource(int priority) {
        this.priority = priority;
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.singletonMap(QUARKUS_HTTP_HOST, getValue(QUARKUS_HTTP_HOST));
    }

    @Override
    public int getOrdinal() {
        return priority;
    }

    @Override
    public String getValue(String propertyName) {
        if (propertyName.equals(QUARKUS_HTTP_HOST)) {
            return LaunchMode.current().isDevOrTest() ? "localhost" : "0.0.0.0";
        }
        return null;
    }

    @Override
    public String getName() {
        return "Quarkus HTTP Host Default Value";
    }
}
