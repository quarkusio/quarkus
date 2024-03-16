package io.quarkus.resteasy.reactive.links.runtime;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class GetterAccessorsContainerRecorder {

    /**
     * Create new getter accessors container.
     */
    public RuntimeValue<GetterAccessorsContainer> newContainer() {
        return new RuntimeValue<>(new GetterAccessorsContainer());
    }

    /**
     * Add a getter accessor to a container.
     */
    public void addAccessor(RuntimeValue<GetterAccessorsContainer> container, String className, String fieldName,
            String accessorName) {
        try {
            // Create a new accessor object early
            GetterAccessor accessor = (GetterAccessor) Thread.currentThread()
                    .getContextClassLoader()
                    .loadClass(accessorName)
                    .getDeclaredConstructor()
                    .newInstance();
            container.getValue().put(className, fieldName, accessor);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize " + accessorName + ": " + e.getMessage());
        }
    }
}
