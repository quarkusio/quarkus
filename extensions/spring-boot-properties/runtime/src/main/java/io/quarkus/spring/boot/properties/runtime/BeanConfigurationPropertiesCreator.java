package io.quarkus.spring.boot.properties.runtime;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.configuration.QuarkusConfigBuilderCustomizer;
import io.smallrye.config.ConfigMappings.ConfigClass;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.spring.ConfigurationPropertiesMappingHandler;

public class BeanConfigurationPropertiesCreator implements BeanCreator<Object> {
    @Override
    public Object create(SyntheticCreationalContext<Object> context) {
        Class<?> type = (Class<?>) context.getParams().get("type");
        String prefix = (String) context.getParams().get("prefix");

        // The instance returned by the @Bean method is the template of the mapping. Its field values are the
        // defaults of the mapping, and the configuration is populated in the instance itself, so final fields and
        // fields without configuration keep the values set by the @Bean method.
        Object template = Arc.container().instance(type, SpringBootConfigProperties.Literal.INSTANCE).get();

        SmallRyeConfig currentConfig = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                .withCustomizers(new QuarkusConfigBuilderCustomizer())
                .withSources(wrapConfig(currentConfig));

        ConfigClass configClass = ConfigClass.configClass(type, prefix, new ConfigurationPropertiesMappingHandler());
        if (type.isInterface()) {
            // an interface cannot be populated, so the instance returned by the @Bean method is ignored
            builder.withMapping(configClass);
        } else {
            builder.withMapping(configClass, template);
        }

        return builder.build().getConfigMapping(type, prefix);
    }

    private static ConfigSource wrapConfig(SmallRyeConfig config) {
        return new ConfigSource() {
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
