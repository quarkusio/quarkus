package io.quarkus.restclient.config;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.ConfigMappingLoader;
import io.smallrye.config.ConfigMappingObject;
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
        Set<String> ignoreNames = getIgnoreNames();
        builder.withInterceptors(new RestClientNameUnquotedFallbackInterceptor(restClients, ignoreNames));
        builder.withInterceptors(new RestClientNameFallbackInterceptor(restClients, ignoreNames));
        for (RegisteredRestClient restClient : restClients) {
            builder.withDefaultValue("quarkus.rest-client.\"" + restClient.getFullName() + "\".force", "true");
            builder.withDefaultValue("quarkus.rest-client." + restClient.getSimpleName() + ".force", "true");
            if (restClient.getConfigKey() != null) {
                builder.withDefaultValue("quarkus.rest-client.\"" + restClient.getConfigKey() + "\".force", "true");
            }
        }
        return builder;
    }

    public abstract List<RegisteredRestClient> getRestClients();

    /**
     * Builds a list of base names from {@link RestClientsConfig} to ignore when rewriting the REST Client
     * configuration. Only configuration from {@link RestClientsConfig#clients()} requires rewriting, but they share
     * the same path of the base names due to {@link io.smallrye.config.WithParentName} in the member.
     *
     * @return a Set with the names to ignore.
     */
    public Set<String> getIgnoreNames() {
        Class<? extends ConfigMappingObject> implementationClass = ConfigMappingLoader
                .getImplementationClass(RestClientsConfig.class);
        return configMappingNames(implementationClass).get(RestClientsConfig.class.getName()).get("")
                .stream()
                .filter(s -> s.charAt(0) != '*')
                .map(s -> "quarkus.rest-client." + s)
                .collect(Collectors.toSet());
    }

    /**
     * TODO - Generate this in RestClientConfigUtils - The list can be collected during build time and generated
     */
    @SuppressWarnings("unchecked")
    @Deprecated(forRemoval = true)
    static <T> Map<String, Map<String, Set<String>>> configMappingNames(final Class<T> implementationClass) {
        try {
            Method getNames = implementationClass.getDeclaredMethod("getNames");
            return (Map<String, Map<String, Set<String>>>) getNames.invoke(null);
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        } catch (InvocationTargetException e) {
            try {
                throw e.getCause();
            } catch (RuntimeException | Error e2) {
                throw e2;
            } catch (Throwable t) {
                throw new UndeclaredThrowableException(t);
            }
        }
    }

    static int indexOfRestClient(final String name) {
        if (name.startsWith("quarkus.rest-client.")) {
            return 20;
        }
        return -1;
    }
}
