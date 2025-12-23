package io.quarkus.jackson.runtime;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.quarkus.jackson.ObjectMapperCustomizer;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.annotations.RuntimeInit;
import io.quarkus.runtime.annotations.StaticInit;
import io.quarkus.runtime.shutdown.ShutdownListener;

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
    public Supplier<ObjectMapperCustomizer> customizerSupplier(Map<Class<?>, Class<?>> mixinsMap) {
        return new Supplier<>() {
            @Override
            public ObjectMapperCustomizer get() {
                return new ObjectMapperCustomizer() {
                    @Override
                    public void customize(ObjectMapper objectMapper) {
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

    @RuntimeInit
    public ShutdownListener clearCachesOnShutdown() {
        return new ShutdownListener() {
            @Override
            public void shutdown(ShutdownNotification notification) {
                TypeFactory.defaultInstance().clearCache();
            }
        };
    }
}
