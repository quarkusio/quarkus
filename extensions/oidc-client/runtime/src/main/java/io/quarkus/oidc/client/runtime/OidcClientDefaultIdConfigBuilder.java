package io.quarkus.oidc.client.runtime;

import static io.quarkus.oidc.client.runtime.OidcClientRecorder.DEFAULT_OIDC_CLIENT_ID;

import java.util.Iterator;
import java.util.OptionalInt;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Priorities;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * Sets default {@link OidcClientConfig#id()} to the client's named key.
 * For example, the configuration property 'quarkus.oidc-client.<<named-key>>.id' is set to the '<<named-key>>' if
 * user did not configure any value.
 */
public class OidcClientDefaultIdConfigBuilder implements ConfigBuilder {

    private static final String OIDC_CLIENT_PREFIX = "quarkus.oidc-client.";
    private static final String ID_POSTFIX = ".id";
    private static final String DEFAULT_CLIENT_ID_PROPERTY_KEY = OIDC_CLIENT_PREFIX + "id";
    private static final String DOUBLE_QUOTE = "\"";
    private static final String WITH_DEFAULTS_ID_KEY = "quarkus.oidc-client.*.id";

    @Override
    public SmallRyeConfigBuilder configBuilder(SmallRyeConfigBuilder builder) {
        final ConfigSourceInterceptor configSourceInterceptor = createConfigSourceInterceptor();
        builder.withInterceptorFactories(new ConfigSourceInterceptorFactory() {

            @Override
            public ConfigSourceInterceptor getInterceptor(ConfigSourceInterceptorContext configSourceInterceptorContext) {
                return configSourceInterceptor;
            }

            @Override
            public OptionalInt getPriority() {
                return OptionalInt.of(Priorities.LIBRARY + 200);
            }
        });
        return builder;
    }

    @Override
    public int priority() {
        return Integer.MIN_VALUE;
    }

    private static boolean isNotSet(ConfigValue configValue) {
        return configValue == null || configValue.getValue() == null || configValue.getValue().isEmpty();
    }

    private static ConfigValue createConfigValue(String name, String value) {
        return ConfigValue.builder().withName(name).withValue(value).build();
    }

    private static ConfigSourceInterceptor createConfigSourceInterceptor() {
        return new ConfigSourceInterceptor() {
            @Override
            public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
                var configValue = context.proceed(name);
                if (isNotSet(configValue) && name.startsWith(OIDC_CLIENT_PREFIX) && name.endsWith(ID_POSTFIX)
                        && !WITH_DEFAULTS_ID_KEY.equals(name)) {
                    if (name.equals(DEFAULT_CLIENT_ID_PROPERTY_KEY)) {
                        return createConfigValue(name, DEFAULT_OIDC_CLIENT_ID);
                    } else {
                        var maybeClientName = name.substring(OIDC_CLIENT_PREFIX.length(), name.length() - ID_POSTFIX.length());
                        // this will cause an issue if the client name contained dot
                        // but the alternative is much more complex because we would have to presume that
                        // there never will never be other property that starts with the 'quarkus.oidc-client.'
                        // and ends with '.id' but is not the client id
                        if (!maybeClientName.contains(".")) {
                            // this is additional named client, now we know that OIDC client extension validates
                            // the 'id' always equals named key, so we can preset this for users
                            if (maybeClientName.startsWith(DOUBLE_QUOTE) && maybeClientName.endsWith(DOUBLE_QUOTE)) {
                                var clientNameWithoutQuotes = maybeClientName.substring(1, maybeClientName.length() - 1);
                                return createConfigValue(name, clientNameWithoutQuotes);
                            }
                            return createConfigValue(name, maybeClientName);
                        }
                    }
                }
                return configValue;
            }

            @Override
            public Iterator<String> iterateNames(ConfigSourceInterceptorContext context) {
                return context.iterateNames();
            }
        };
    }
}
