package io.quarkus.jackson.runtime;

import java.util.Optional;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class JacksonSupportRecorder {

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
                                                Thread.currentThread().getContextClassLoader())
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
}
