package io.quarkus.restclient.config;

import static io.quarkus.restclient.config.AbstractRestClientConfigBuilder.indexOfRestClient;
import static io.smallrye.config.ProfileConfigSourceInterceptor.convertProfile;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import jakarta.annotation.Priority;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.config.ConfigValue;
import io.smallrye.config.FallbackConfigSourceInterceptor;
import io.smallrye.config.NameIterator;
import io.smallrye.config.Priorities;

/**
 * Relocates unquoted config keys to quoted
 * <p>
 * In the case of {@link RegisterRestClient#configKey()}, users either use quoted or unquoted configuration names for
 * single config key segments. Again, the Config system does not support such a feature (but could be implemented), so
 * the interceptor also relocates to unquoted configuration names.
 * <p>
 * We need a double-way relocation / fallback mapping between unquoted and quoted because SmallRye Config will use the
 * first distict key it finds to populate {@link RestClientsConfig#clients()} in the list of property names. If quoted,
 * it will search for all quoted. If unquoted, it will search for all unquoted. We cannot be sure how the user sets the
 * configuration, especially considering that we may not be able to query the list directly if the config comes from a
 * source that does not support listing property names.
 */
@Priority(Priorities.LIBRARY + 605)
public class RestClientNameUnquotedFallbackInterceptor extends FallbackConfigSourceInterceptor {
    public RestClientNameUnquotedFallbackInterceptor(final List<RegisteredRestClient> restClients,
            final Set<String> ignoreNames) {
        super(relocate(restClients, ignoreNames));
    }

    private static Function<String, String> relocate(final List<RegisteredRestClient> restClients,
            final Set<String> ignoreNames) {
        return new Function<String, String>() {
            @Override
            public String apply(final String name) {
                int indexOfRestClient = indexOfRestClient(name);
                if (indexOfRestClient != -1) {
                    if (ignoreNames.contains(name)) {
                        return name;
                    }

                    for (RegisteredRestClient restClient : restClients) {
                        String configKey = restClient.getConfigKey();
                        if (configKey == null || configKey.isEmpty() || restClient.isConfigKeySegments()) {
                            continue;
                        }

                        int endOfConfigKey = indexOfRestClient + configKey.length();
                        if (name.regionMatches(indexOfRestClient, configKey, 0, configKey.length())) {
                            if (name.length() > endOfConfigKey && name.charAt(endOfConfigKey) == '.') {
                                return "quarkus.rest-client.\"" + configKey + "\"" + name.substring(endOfConfigKey);
                            }
                        }
                    }
                }
                return name;
            }
        };
    }

    private static final Comparator<ConfigValue> CONFIG_SOURCE_COMPARATOR = new Comparator<ConfigValue>() {
        @Override
        public int compare(ConfigValue original, ConfigValue candidate) {
            int result = Integer.compare(original.getConfigSourceOrdinal(), candidate.getConfigSourceOrdinal());
            if (result != 0) {
                return result;
            }
            result = Integer.compare(original.getConfigSourcePosition(), candidate.getConfigSourcePosition()) * -1;
            if (result != 0) {
                return result;
            }
            // If both properties are profiled, prioritize the one with the most specific profile.
            if (original.getName().charAt(0) == '%' && candidate.getName().charAt(0) == '%') {
                List<String> originalProfiles = convertProfile(
                        new NameIterator(original.getName()).getNextSegment().substring(1));
                List<String> candidateProfiles = convertProfile(
                        new NameIterator(candidate.getName()).getNextSegment().substring(1));
                return Integer.compare(originalProfiles.size(), candidateProfiles.size()) * -1;
            }
            return result;
        }
    };
}
