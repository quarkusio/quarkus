package io.quarkus.runtime.configuration;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.SecretKeysHandler;
import io.smallrye.config.SecretKeysHandlerFactory;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

/**
 * Convenience helper to generate the {@link SmallRyeConfigBuilderCustomizer} bytecode, by wrapping methods that
 * require varargs or collections as parameters.
 */
public abstract class AbstractConfigBuilder implements SmallRyeConfigBuilderCustomizer {
    protected static void withDefaultValue(SmallRyeConfigBuilder builder, String name, String value) {
        builder.withDefaultValue(name, value);
    }

    // TODO - radcortez - Can be improved by avoiding introspection work in the Converter class.
    // Not a big issue, because registering Converters via ServiceLoader is not a common case
    protected static void withConverter(SmallRyeConfigBuilder builder, Converter<?> converter) {
        builder.withConverters(new Converter[] { converter });
    }

    protected static void withInterceptor(SmallRyeConfigBuilder builder, ConfigSourceInterceptor interceptor) {
        builder.withInterceptors(interceptor);
    }

    protected static void withInterceptorFactory(
            SmallRyeConfigBuilder builder,
            ConfigSourceInterceptorFactory interceptorFactory) {
        builder.withInterceptorFactories(interceptorFactory);
    }

    protected static void withSource(SmallRyeConfigBuilder builder, ConfigSource configSource) {
        builder.withSources(configSource);
    }

    protected static void withSource(SmallRyeConfigBuilder builder, ConfigSourceProvider configSourceProvider) {
        builder.withSources(configSourceProvider);
    }

    protected static void withSource(SmallRyeConfigBuilder builder, ConfigSourceFactory configSourceFactory) {
        builder.withSources(configSourceFactory);
    }

    protected static void withSecretKeyHandler(SmallRyeConfigBuilder builder, SecretKeysHandler secretKeysHandler) {
        builder.withSecretKeysHandlers(secretKeysHandler);
    }

    protected static void withSecretKeyHandler(SmallRyeConfigBuilder builder,
            SecretKeysHandlerFactory secretKeysHandlerFactory) {
        builder.withSecretKeyHandlerFactories(secretKeysHandlerFactory);
    }

    protected static void withMapping(SmallRyeConfigBuilder builder, String mappingClass, String prefix) {
        try {
            // To support mappings that are not public
            builder.withMapping(builder.getClassLoader().loadClass(mappingClass), prefix);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    protected static void withBuilder(SmallRyeConfigBuilder builder, ConfigBuilder configBuilder) {
        builder.withCustomizers(new SmallRyeConfigBuilderCustomizer() {
            @Override
            public void configBuilder(final SmallRyeConfigBuilder builder) {
                configBuilder.configBuilder(builder);
            }

            @Override
            public int priority() {
                return configBuilder.priority();
            }
        });
    }

    protected static void withCustomizer(SmallRyeConfigBuilder builder, SmallRyeConfigBuilderCustomizer customizer) {
        builder.withCustomizers(customizer);
    }

    @SuppressWarnings("unchecked")
    public static void withCustomizer(SmallRyeConfigBuilder builder, String customizer) {
        try {
            Class<SmallRyeConfigBuilderCustomizer> customizerClass = (Class<SmallRyeConfigBuilderCustomizer>) builder
                    .getClassLoader().loadClass(customizer);
            customizerClass.getDeclaredConstructor().newInstance().configBuilder(builder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
