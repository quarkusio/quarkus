package io.quarkus.context.test.customContext;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

/**
 * Simple context provider for {@link CustomContext}
 */
public class CustomContextProvider implements ThreadContextProvider {

    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        return snapshot("");
    }

    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        return snapshot(CustomContext.get());
    }

    public String getThreadContextType() {
        return CustomContext.NAME;
    }

    private ThreadContextSnapshot snapshot(String label) {
        return () -> {
            String labelToRestore = CustomContext.get();
            AtomicBoolean restored = new AtomicBoolean();

            // Construct an instance that restores the previous context that was on the thread
            // prior to applying the specified 'label' as the new context.
            ThreadContextController contextRestorer = () -> {
                if (restored.compareAndSet(false, true)) {
                    CustomContext.set(labelToRestore);
                } else {
                    throw new IllegalStateException();
                }
            };

            CustomContext.set(label);
            return contextRestorer;
        };
    }
}
