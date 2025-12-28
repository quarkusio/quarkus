package io.quarkus.arc.runtime;

import java.util.function.Supplier;

import io.quarkus.registry.ValueRegistry;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

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
}
