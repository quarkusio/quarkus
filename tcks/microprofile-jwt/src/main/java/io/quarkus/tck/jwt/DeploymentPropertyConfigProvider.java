package io.quarkus.tck.jwt;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.*;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import io.quarkus.runtime.util.ClassPathUtils;
import io.smallrye.config.PropertiesConfigSource;

public class DeploymentPropertyConfigProvider implements ConfigSourceProvider {

    private List<ConfigSource> configSources = new ArrayList<>();

    public DeploymentPropertyConfigProvider() {
        try {
            Enumeration<URL> propertyFileUrls = Thread.currentThread().getContextClassLoader()
                    .getResources("META-INF/microprofile-config.properties");

            while (propertyFileUrls.hasMoreElements()) {
                URL propertyFileUrl = propertyFileUrls.nextElement();
                if (propertyFileUrl.toString().contains("quarkus-arquillian")) {
                    ClassPathUtils.consumeStream(propertyFileUrl, in -> {
                        try {
                            Properties p = new Properties();
                            p.load(in);
                            configSources.add(new PropertiesConfigSource(new HashMap(p), propertyFileUrl.toString(), 110));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
                }
            }

        } catch (IOException e) {
            throw new IllegalStateException("problem while loading microprofile-config.properties files", e);
        }
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        return configSources;
    }
}
