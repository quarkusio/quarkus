package io.quarkus.resteasy.common.runtime.config;

import org.jboss.resteasy.microprofile.config.FilterConfigSource;
import org.jboss.resteasy.microprofile.config.ServletConfigSource;
import org.jboss.resteasy.microprofile.config.ServletContextConfigSource;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * Config Sources required to start RESTEasy. <br>
 * Because STATIC INIT, does not auto discover sources, these have to be registered manually. <br>
 * For RUNTIME Config, these sources are auto-discovered via the ServiceLoader mechanism, so no registration is
 * required. Ideally, to keep consistency, we should also manually register these sources for RUNTIME Config, but we
 * don't have control in the ServiceLoader files provided by RESTEasy.
 */
public class ResteasyConfigBuilder implements ConfigBuilder {
    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
        return builder.withSources(new ServletConfigSource()).withSources(new FilterConfigSource())
                .withSources(new ServletContextConfigSource());
    }
}
