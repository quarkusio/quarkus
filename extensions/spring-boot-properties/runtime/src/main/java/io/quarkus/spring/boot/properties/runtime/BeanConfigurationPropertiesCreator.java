package io.quarkus.spring.boot.properties.runtime;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.configuration.QuarkusConfigBuilderCustomizer;
import io.smallrye.config.ConfigMappingHandler;
import io.smallrye.config.ConfigMappings.ConfigClass;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;
import io.smallrye.config.spring.ConfigurationPropertiesMappingHandler;

public class BeanConfigurationPropertiesCreator implements BeanCreator<Object> {
    @Override
    public Object create(SyntheticCreationalContext<Object> context) {
        Class<?> type = (Class<?>) context.getParams().get("type");
        String prefix = (String) context.getParams().get("prefix");
        String extractorClassName = (String) context.getParams().get("extractor");

        Object factoryBean = Arc.container()
                .instance(type, SpringBootConfigProperties.Literal.INSTANCE).get();

        Map<String, String> beanValues = extractValues(extractorClassName, factoryBean);

        SmallRyeConfig currentConfig = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        ConfigMappingHandler handler = new ConfigMappingHandler() {
            static final ConfigurationPropertiesMappingHandler DELEGATE = new ConfigurationPropertiesMappingHandler();

            @Override
            public boolean handles(Class<?> classType) {
                return DELEGATE.handles(classType);
            }

            @Override
            public FieldMember processField(Field field) {
                return DELEGATE.processField(field);
            }

            @Override
            public String getPrefix(Class<?> classType) {
                return prefix;
            }

            @Override
            public boolean ignoreUnmappedProperties() {
                return DELEGATE.ignoreUnmappedProperties();
            }
        };

        SmallRyeConfig derivedConfig = new SmallRyeConfigBuilder()
                .withCustomizers(new QuarkusConfigBuilderCustomizer())
                .withMapping(ConfigClass.configClass(type, handler))
                .withSources(wrapConfig(currentConfig))
                .withSources(new MapBackedConfigSource("Bean Defaults", beanValues, Integer.MIN_VALUE) {
                })
                .build();

        return derivedConfig.getConfigMapping(type, prefix);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> extractValues(String extractorClassName, Object instance) {
        try {
            Class<?> extractorClass = Thread.currentThread().getContextClassLoader().loadClass(extractorClassName);
            Method extractMethod = extractorClass.getMethod("extract", Object.class);
            return (Map<String, String>) extractMethod.invoke(null, instance);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                | InvocationTargetException e) {
            throw new RuntimeException("Failed to extract bean property values", e);
        }
    }

    private static org.eclipse.microprofile.config.spi.ConfigSource wrapConfig(SmallRyeConfig config) {
        return new org.eclipse.microprofile.config.spi.ConfigSource() {
            @Override
            public Set<String> getPropertyNames() {
                Set<String> properties = new HashSet<>();
                config.getPropertyNames().forEach(properties::add);
                return properties;
            }

            @Override
            public String getValue(String propertyName) {
                return config.getConfigValue(propertyName).getValue();
            }

            @Override
            public String getName() {
                return "Current Config";
            }
        };
    }
}
