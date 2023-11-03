package io.quarkus.jackson.runtime;

import java.util.Map;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.jackson.ObjectMapperCustomizer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class MixinsRecorder {

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
}
