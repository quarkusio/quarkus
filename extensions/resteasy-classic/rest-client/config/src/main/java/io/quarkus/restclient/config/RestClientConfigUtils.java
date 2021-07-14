package io.quarkus.restclient.config;

import static io.quarkus.restclient.config.Constants.MP_REST_SCOPE_FORMAT;
import static io.quarkus.restclient.config.Constants.QUARKUS_REST_SCOPE_FORMAT;

import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.jboss.jandex.ClassInfo;

public final class RestClientConfigUtils {

    private RestClientConfigUtils() {
    }

    public static Optional<String> determineConfiguredScope(Config config, ClassInfo restClientInterface,
            Optional<String> configKey) {
        Optional<String> scopeConfig;
        if (configKey.isPresent()) { // if configKey is defined in the @RestClient annotation
            // quarkus style config; configKey
            scopeConfig = config.getOptionalValue(String.format(QUARKUS_REST_SCOPE_FORMAT, configKey.get()),
                    String.class);
            if (scopeConfig.isEmpty()) { // microprofile style config; configKey
                scopeConfig = config.getOptionalValue(String.format(MP_REST_SCOPE_FORMAT, configKey), String.class);
            }
        } else {
            // quarkus style config; fully qualified class name
            scopeConfig = config.getOptionalValue(
                    String.format(QUARKUS_REST_SCOPE_FORMAT, '"' + restClientInterface.name().toString() + '"'),
                    String.class);
            if (scopeConfig.isEmpty()) { // quarkus style config; short class name
                scopeConfig = config.getOptionalValue(
                        String.format(QUARKUS_REST_SCOPE_FORMAT, restClientInterface.simpleName()),
                        String.class);
            }
            if (scopeConfig.isEmpty()) { // microprofile style config; fully qualified class name
                scopeConfig = config.getOptionalValue(
                        String.format(MP_REST_SCOPE_FORMAT, restClientInterface.name().toString()),
                        String.class);
            }
        }
        return scopeConfig;
    }
}
