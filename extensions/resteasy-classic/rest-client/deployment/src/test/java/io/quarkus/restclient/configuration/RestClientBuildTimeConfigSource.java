package io.quarkus.restclient.configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.common.MapBackedConfigSource;

public class RestClientBuildTimeConfigSource extends MapBackedConfigSource {
    public RestClientBuildTimeConfigSource() {
        super(RestClientBuildTimeConfigSource.class.getName(), new HashMap<>());
    }

    @Override
    public String getValue(final String propertyName) {
        if (!propertyName.equals("io.quarkus.restclient.configuration.EchoClient/mp-rest/url")) {
            return null;
        }

        if (isBuildTime()) {
            return "http://nohost:${quarkus.http.test-port:8081}";
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

    private static boolean isBuildTime() {
        for (ConfigSource configSource : ConfigProvider.getConfig().getConfigSources()) {
            if (configSource.getClass().getSimpleName().equals("BuildTimeEnvConfigSource")) {
                return true;
            }
        }
        return false;
    }
}
