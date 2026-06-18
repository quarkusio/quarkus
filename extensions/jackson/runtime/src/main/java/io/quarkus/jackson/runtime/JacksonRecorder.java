package io.quarkus.jackson.runtime;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import io.quarkus.jackson.JsonMapperBuilderCustomizer;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.annotations.StaticInit;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

@Recorder
public class JacksonRecorder {

    @StaticInit
    public Supplier<JacksonSupport> supplier(Optional<String> propertyNamingStrategyClassName) {
        return new Supplier<>() {
            @Override
            public JacksonSupport get() {
                return new JacksonSupport() {
                    @Override
                    public Optional<PropertyNamingStrategies.NamingBase> configuredNamingStrategy() {
                        if (propertyNamingStrategyClassName.isPresent()) {
                            try {
                                var value = (PropertyNamingStrategies.NamingBase) Class
                                        .forName(propertyNamingStrategyClassName.get(), true,
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
        };
    }

    @StaticInit
    public Supplier<JsonMapperBuilderCustomizer> customizerSupplier(Map<Class<?>, Class<?>> mixinsMap) {
        return new Supplier<>() {
            @Override
            public JsonMapperBuilderCustomizer get() {
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
        };
    }

}
