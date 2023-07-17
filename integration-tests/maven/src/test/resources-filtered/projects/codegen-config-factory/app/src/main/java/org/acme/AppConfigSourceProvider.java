package org.acme;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import io.smallrye.config.PropertiesConfigSource;

public class AppConfigSourceProvider implements ConfigSourceProvider {

	@Override
	public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
		return List.of(new PropertiesConfigSource(Map.of(AcmeConstants.ACME_CONFIG_PROVIDER_PROP, getClass().getName()), getClass().getName(), 150));
	}
}
