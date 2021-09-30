package io.quarkus.restclient.config;

import static io.quarkus.restclient.config.Constants.MP_REST_SCOPE_FORMAT;
import static io.quarkus.restclient.config.Constants.QUARKUS_REST_SCOPE_FORMAT;

import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.jboss.jandex.ClassInfo;

public final class RestClientConfigUtils {

    private RestClientConfigUtils() {
    }

    public static Optional<String> findConfiguredScope(Config config, ClassInfo restClientInterface,
            Optional<String> configKeyOptional) {
        Optional<String> scopeConfig;

        // quarkus style config; fully qualified class name
        scopeConfig = config.getOptionalValue(
                String.format(QUARKUS_REST_SCOPE_FORMAT, '"' + restClientInterface.name().toString() + '"'),
                String.class);
        if (scopeConfig.isEmpty()) { // microprofile style config; fully qualified class name
            scopeConfig = config.getOptionalValue(
                    String.format(MP_REST_SCOPE_FORMAT, restClientInterface.name().toString()),
                    String.class);
        }
        if (scopeConfig.isEmpty() && configKeyOptional.isPresent()) { // quarkus style config; configKey
            scopeConfig = config.getOptionalValue(String.format(QUARKUS_REST_SCOPE_FORMAT, configKeyOptional.get()),
                    String.class);
        }
        if (scopeConfig.isEmpty() && configKeyOptional.isPresent()) { // microprofile style config; configKey
            scopeConfig = config.getOptionalValue(String.format(MP_REST_SCOPE_FORMAT, configKeyOptional.get()), String.class);
        }
        if (scopeConfig.isEmpty()) { // quarkus style config; short class name
            scopeConfig = config.getOptionalValue(
                    String.format(QUARKUS_REST_SCOPE_FORMAT, restClientInterface.simpleName()),
                    String.class);
        }
        return scopeConfig;
    }

}
