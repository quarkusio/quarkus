package io.quarkus.restclient.configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.common.MapBackedConfigSource;

@StaticInitSafe
public class RestClientRunTimeConfigSource extends MapBackedConfigSource {
    public RestClientRunTimeConfigSource() {
        super(RestClientRunTimeConfigSource.class.getName(), new HashMap<>());
    }

    @Override
    public String getValue(final String propertyName) {
        if (!propertyName.equals("io.quarkus.restclient.configuration.EchoClient/mp-rest/url")) {
            return null;
        }

        if (isRuntime()) {
            return "http://localhost:${quarkus.http.test-port:8081}";
        }

        return null;
    }

    @Override
    public Set<String> getPropertyNames() {
        return Collections.singleton("io.quarkus.restclient.configuration.EchoClient/mp-rest/url");
    }

    @Override
    public int getOrdinal() {
        return Integer.MAX_VALUE;
    }

    private static boolean isRuntime() {
        for (ConfigSource configSource : ConfigProvider.getConfig().getConfigSources()) {
            if (configSource.getName().equals("PropertiesConfigSource[source=Specified default values]")) {
                return true;
            }
        }
        return false;
    }
}
