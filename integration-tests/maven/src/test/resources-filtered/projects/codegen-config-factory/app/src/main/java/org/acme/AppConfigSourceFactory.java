package org.acme;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.PropertiesConfigSource;

public class AppConfigSourceFactory implements ConfigSourceFactory {

	@Override
	public Iterable<ConfigSource> getConfigSources(ConfigSourceContext context) {
		return List.of(new PropertiesConfigSource(Map.of(AcmeConstants.ACME_CONFIG_FACTORY_PROP, getClass().getName()), getClass().getName(), 150));
	}
}
