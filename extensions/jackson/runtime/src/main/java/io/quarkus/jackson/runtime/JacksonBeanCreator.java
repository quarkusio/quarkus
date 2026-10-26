package io.quarkus.jackson.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkus.jackson.JsonMapperBuilderCustomizer;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

public final class JacksonBeanCreator {

    private JacksonBeanCreator() {
    }

    public static JacksonSupport jacksonSupport(String propertyNamingStrategyClassName) {
        return new JacksonSupport() {
            @Override
            public Optional<PropertyNamingStrategies.NamingBase> configuredNamingStrategy() {
                if (propertyNamingStrategyClassName != null) {
                    try {
                        var value = (PropertyNamingStrategies.NamingBase) Class
                                .forName(propertyNamingStrategyClassName, true,
                                        Thread.currentThread()
                                                .getContextClassLoader())
                                .getDeclaredConstructor().newInstance();
                        return Optional.of(value);
                    } catch (Exception e) {
                        // shouldn't happen as propertyNamingStrategyClassName is validated at build time
                        throw new RuntimeException(e);
                    }
                }
                return Optional.empty();
            }
        };
    }

    public static JsonMapperBuilderCustomizer mixinsCustomizer(Map<Class<?>, Class<?>> mixinsMap) {
        return new JsonMapperBuilderCustomizer() {
            @Override
            public void customize(JsonMapper.Builder objectMapper) {
                for (var entry : mixinsMap.entrySet()) {
                    objectMapper.addMixIn(entry.getKey(), entry.getValue());
                }
            }

            @Override
            public int priority() {
                return DEFAULT_PRIORITY + 1;
            }
        };
    }
}
