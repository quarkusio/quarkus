package io.quarkus.runtime.configuration;

import java.util.Map;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.config.ConfigMappingLoader;
import io.smallrye.config.ConfigMappings.ConfigClass;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.SecretKeysHandler;
import io.smallrye.config.SecretKeysHandlerFactory;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

/**
 * Convenience helper to generate the {@link SmallRyeConfigBuilderCustomizer} bytecode, by wrapping methods that
 * require varargs or collections as parameters.
 */
public abstract class AbstractConfigBuilder implements SmallRyeConfigBuilderCustomizer {

    protected static void withDefaultValues(SmallRyeConfigBuilder builder, Map<String, String> values) {
        builder.withDefaultValues(values);
    }

    @SuppressWarnings("unchecked")
    protected static <T> void withConverter(SmallRyeConfigBuilder builder, String type, int priority, Converter<T> converter) {
        try {
            // To support converters that are not public
            builder.withConverter((Class<T>) Class.forName(type, false, builder.getClassLoader()), priority, converter);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
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

    protected static void withMapping(SmallRyeConfigBuilder builder, ConfigClass mapping) {
        builder.withMapping(mapping);
    }

    protected static void withMapping(SmallRyeConfigBuilder builder, String mappingClass, String prefix) {
        try {
            // To support mappings that are not public
            builder.withMapping(builder.getClassLoader().loadClass(mappingClass), prefix);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    protected static void withMappingInstance(SmallRyeConfigBuilder builder, ConfigClass mapping) {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        builder.getMappingsBuilder().mappingInstance(mapping, config.getConfigMapping(mapping.getType(), mapping.getPrefix()));
    }

    protected static void withMappingIgnore(SmallRyeConfigBuilder builder, String path) {
        builder.withMappingIgnore(path);
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

    public static ConfigClass configClass(final String mappingClass, final String prefix) {
        try {
            // To support mappings that are not public
            Class<?> klass = Thread.currentThread().getContextClassLoader().loadClass(mappingClass);
            return ConfigClass.configClass(klass, prefix);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void ensureLoaded(final String mappingClass) {
        try {
            // To support mappings that are not public
            Class<?> klass = Thread.currentThread().getContextClassLoader().loadClass(mappingClass);
            ConfigMappingLoader.ensureLoaded(klass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
