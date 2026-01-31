package io.quarkus.arc.runtime;

import java.util.function.Supplier;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.value.registry.ValueRegistry;
import io.quarkus.value.registry.ValueRegistry.RuntimeKey;

@Recorder
public class ValueRegistryRecorder {
    private final RuntimeValue<ValueRegistry> valueRegistry;

    public ValueRegistryRecorder(RuntimeValue<ValueRegistry> valueRegistry) {
        this.valueRegistry = valueRegistry;
    }

    public Supplier<ValueRegistry> valueRegistry() {
        return new Supplier<>() {
            @Override
            public ValueRegistry get() {
                return valueRegistry.getValue();
            }
        };
    }

    public <T> Supplier<T> runtimeInfo(final Class<T> runtimeInfo) {
        return new Supplier<>() {
            @Override
            public T get() {
                return valueRegistry.getValue().get(RuntimeKey.key(runtimeInfo));
            }
        };
    }
}
