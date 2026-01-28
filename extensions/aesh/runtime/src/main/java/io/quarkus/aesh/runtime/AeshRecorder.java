package io.quarkus.aesh.runtime;

import java.util.List;
import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;

/**
 * Recorder that captures build-time command metadata and makes it available at runtime
 * as a synthetic {@link AeshContext} CDI bean.
 */
@Recorder
public class AeshRecorder {

    /**
     * Creates a supplier for the {@link AeshContext} synthetic bean.
     * <p>
     * Called at build time by {@code AeshProcessor.recordAeshMetadata()}.
     * The returned supplier is invoked at runtime to create the context instance.
     *
     * @param commands list of mutable command metadata (mutable for recorder serialization)
     * @param modeName the resolved mode name ("console" or "runtime")
     * @return supplier that creates an immutable AeshContext at runtime
     */
    public Supplier<Object> createContext(List<AeshCommandMetadata> commands, String modeName) {
        List<AeshCommandMetadata> immutableCommands = List.copyOf(commands);
        AeshMode mode = AeshMode.valueOf(modeName);

        return new Supplier<Object>() {
            @Override
            public Object get() {
                return new AeshContext() {
                    @Override
                    public List<AeshCommandMetadata> getCommands() {
                        return immutableCommands;
                    }

                    @Override
                    public AeshMode getMode() {
                        return mode;
                    }
                };
            }
        };
    }
}
