package io.quarkus.oidc.runtime;

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
 * Sets default {@link OidcTenantConfig#tenantId()} to the tenant's named key.
 * For example, the configuration property 'quarkus.oidc.<<named-key>>.tenant-id' is set to the '<<named-key>>' if
 * user did not configure any value.
 */
public class OidcTenantDefaultIdConfigBuilder implements ConfigBuilder {

    private static final String OIDC_PREFIX = "quarkus.oidc.";
    private static final String TENANT_ID_POSTFIX = ".tenant-id";
    private static final String DEFAULT_TENANT_ID_PROPERTY_KEY = OIDC_PREFIX + "tenant-id";
    private static final String DOUBLE_QUOTE = "\"";
    private static final String WITH_DEFAULTS_ID_KEY = "quarkus.oidc.*.tenant-id";

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
                if (isNotSet(configValue) && name.startsWith(OIDC_PREFIX) && name.endsWith(TENANT_ID_POSTFIX)
                        && !WITH_DEFAULTS_ID_KEY.equals(name)) {
                    if (name.equals(DEFAULT_TENANT_ID_PROPERTY_KEY)) {
                        return createConfigValue(name, OidcUtils.DEFAULT_TENANT_ID);
                    } else {
                        var maybeTenantName = name.substring(OIDC_PREFIX.length(), name.length() - TENANT_ID_POSTFIX.length());
                        // this is additional named tenant, now we know that OIDC tenant extension validates
                        // the 'tenant-id' always equals named key, so we can preset this for users
                        if (maybeTenantName.startsWith(DOUBLE_QUOTE) && maybeTenantName.endsWith(DOUBLE_QUOTE)) {
                            var tenantNameWithoutQuotes = maybeTenantName.substring(1, maybeTenantName.length() - 1);
                            return createConfigValue(name, tenantNameWithoutQuotes);
                        } else if (!maybeTenantName.contains(".")) {
                            return createConfigValue(name, maybeTenantName);
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
