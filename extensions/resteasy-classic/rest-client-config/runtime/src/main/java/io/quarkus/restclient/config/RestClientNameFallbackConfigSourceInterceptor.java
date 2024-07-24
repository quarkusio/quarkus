package io.quarkus.restclient.config;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.smallrye.config.ConfigMappingLoader;
import io.smallrye.config.ConfigMappingObject;
import io.smallrye.config.FallbackConfigSourceInterceptor;

/**
 * Fallbacks REST Client FQN to Simple Name.
 * <p>
 * Ideally, this shouldn't be required. The old custom implementation allowed us to mix both names and merge them in a
 * final configuration to use in the REST Client. The standard Config system does not support such a feature. If a
 * configuration supports multiple names, the user has to use the same name across all configuration sources. No other
 * Quarkus extension behaves this way because only the REST Client extension provides the custom code to make it work.
 */
public class RestClientNameFallbackConfigSourceInterceptor extends FallbackConfigSourceInterceptor {
    public RestClientNameFallbackConfigSourceInterceptor(final List<RegisteredRestClient> restClients) {
        super(fallback(restClients));
    }

    private static Function<String, String> fallback(final List<RegisteredRestClient> restClients) {
        Class<? extends ConfigMappingObject> implementationClass = ConfigMappingLoader
                .getImplementationClass(RestClientsConfig.class);
        Set<String> ignoreNames = configMappingNames(implementationClass).get(RestClientsConfig.class.getName()).get("")
                .stream()
                .filter(s -> s.charAt(0) != '*')
                .map(s -> "quarkus.rest-client." + s)
                .collect(Collectors.toSet());

        return new Function<String, String>() {
            @Override
            public String apply(final String name) {
                if (name.startsWith("quarkus.rest-client.")) {
                    if (ignoreNames.contains(name)) {
                        return name;
                    }

                    for (RegisteredRestClient restClient : restClients) {
                        if (name.length() > 20 && name.charAt(20) == '"') {
                            String interfaceName = restClient.getFullName();
                            if (name.regionMatches(21, interfaceName, 0, interfaceName.length())) {
                                if (name.length() > 21 + interfaceName.length()
                                        && name.charAt(21 + interfaceName.length()) == '"') {
                                    return "quarkus.rest-client." + restClient.getSimpleName()
                                            + name.substring(21 + interfaceName.length() + 1);
                                }
                            }
                        }
                    }
                }
                return name;
            }
        };
    }

    /**
     * Expose this as a public API in SmallRye Config
     */
    @SuppressWarnings("unchecked")
    @Deprecated(forRemoval = true)
    private static <T> Map<String, Map<String, Set<String>>> configMappingNames(final Class<T> implementationClass) {
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
}
