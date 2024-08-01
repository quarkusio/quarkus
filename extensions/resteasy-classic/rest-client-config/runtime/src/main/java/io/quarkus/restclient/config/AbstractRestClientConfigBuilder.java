package io.quarkus.restclient.config;

import java.util.List;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * Registers and force load REST Client configuration.
 * <p>
 * Usually, named configuration is mapped using a <code>Map</code> because the names are dynamic and unknown to
 * Quarkus. In the case of the REST Client, configuration names are fixed and known at build time, but not to the point
 * where the names can be mapped statically, so they still need to be mapped in a <code>Map</code>.
 * <p>
 * To populate a <code>Map</code>, because the names are dynamic, the Config system has to rely on the list of
 * property names provided by each source. This also applies to the REST Client, but since the names are known to
 * Quarkus, the REST Client configuration could be loaded even for sources that don't provide a list of property
 * names. To achieve such behaviour, we provide a dummy configuration under each REST Client name to force
 * the Config system to look up the remaining configuration in the same tree.
 * <p>
 * The concrete implementation is bytecode generated in
 * <code>io.quarkus.restclient.config.deployment.RestClientConfigUtils#generateRestClientConfigBuilder</code>
 */
public abstract class AbstractRestClientConfigBuilder implements ConfigBuilder {
    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
        List<RegisteredRestClient> restClients = getRestClients();
        builder.withInterceptors(new RestClientNameFallbackConfigSourceInterceptor(restClients));
        for (RegisteredRestClient restClient : restClients) {
            builder.withDefaultValue("quarkus.rest-client.\"" + restClient.getFullName() + "\".force", "true");
            builder.withDefaultValue("quarkus.rest-client." + restClient.getSimpleName() + ".force", "true");
            if (restClient.getConfigKey() != null) {
                builder.withDefaultValue("quarkus.rest-client." + restClient.getConfigKey() + ".force", "true");
            }
        }
        return builder;
    }

    public abstract List<RegisteredRestClient> getRestClients();
}
