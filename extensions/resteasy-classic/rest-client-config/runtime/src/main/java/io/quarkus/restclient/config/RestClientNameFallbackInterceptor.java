package io.quarkus.restclient.config;

import static io.quarkus.restclient.config.AbstractRestClientConfigBuilder.indexOfRestClient;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import jakarta.annotation.Priority;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.config.FallbackConfigSourceInterceptor;
import io.smallrye.config.Priorities;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * Fallbacks REST Client FQN to Simple Name and quoted config keys to unquoted
 * <p>
 * Ideally, this shouldn't be required. The old custom implementation allowed us to mix both FQN and Simple Name in a
 * merged configuration to use in the REST Client. The standard Config system does not support such a feature. If a
 * configuration supports multiple names, the user has to use the same name across all configuration sources. No other
 * Quarkus extension behaves this way because only the REST Client extension provides the custom code to make it work.
 * <p>
 * In the case of {@link RegisterRestClient#configKey()}, users either use quoted or unquoted configuration names for
 * single config key segments. Again, the Config system does not support such a feature (but could be implemented), so
 * the interceptor also fallbacks to unquoted configuration names, due to the <code>force</code> property added by
 * {@link AbstractRestClientConfigBuilder#configBuilder(SmallRyeConfigBuilder)}.
 */
@Priority(Priorities.LIBRARY + 610)
public class RestClientNameFallbackInterceptor extends FallbackConfigSourceInterceptor {
    public RestClientNameFallbackInterceptor(final List<RegisteredRestClient> restClients,
            final Set<String> ignoreNames) {
        super(fallback(restClients, ignoreNames));
    }

    private static Function<String, String> fallback(final List<RegisteredRestClient> restClients,
            final Set<String> ignoreNames) {
        return new Function<String, String>() {
            @Override
            public String apply(final String name) {
                int indexOfRestClient = indexOfRestClient(name);
                if (indexOfRestClient != -1) {
                    if (ignoreNames.contains(name)) {
                        return name;
                    }

                    int endOfRestClient = indexOfRestClient + 1;
                    for (RegisteredRestClient restClient : restClients) {
                        if (name.length() > indexOfRestClient && name.charAt(indexOfRestClient) == '"') {
                            String interfaceName = restClient.getFullName();
                            if (name.regionMatches(endOfRestClient, interfaceName, 0, interfaceName.length())) {
                                if (name.length() > endOfRestClient + interfaceName.length()
                                        && name.charAt(endOfRestClient + interfaceName.length()) == '"') {
                                    return "quarkus.rest-client." + restClient.getSimpleName()
                                            + name.substring(endOfRestClient + interfaceName.length() + 1);
                                }
                            }

                            String configKey = restClient.getConfigKey();
                            if (configKey == null || configKey.isEmpty() || restClient.isConfigKeySegments()) {
                                continue;
                            }
                            int endOfConfigKey = endOfRestClient + configKey.length();
                            if (name.regionMatches(endOfRestClient, configKey, 0, configKey.length())) {
                                if (name.length() > endOfConfigKey && name.charAt(endOfConfigKey) == '"') {
                                    return "quarkus.rest-client." + configKey + name.substring(endOfConfigKey + 1);
                                }
                            }
                        }
                    }
                }
                return name;
            }
        };
    }
}
