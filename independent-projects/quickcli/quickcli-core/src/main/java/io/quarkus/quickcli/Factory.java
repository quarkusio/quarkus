package io.quarkus.quickcli;

import java.util.function.Supplier;

import io.quarkus.quickcli.model.CommandModel;
import io.quarkus.quickcli.model.CommandModelRegistry;

/**
 * Factory for creating command instances. Allows integration with dependency injection frameworks.
 */
public interface Factory {

    /**
     * Creates an instance of the specified class.
     *
     * @param cls the class to instantiate
     * @return the new instance
     * @throws Exception if instantiation fails
     */
    <T> T create(Class<T> cls) throws Exception;

    /**
     * Default factory that delegates to the generated {@link CommandModel#createInstance()}
     * or a build-time registered instance creator. No reflection is used.
     */
    class DefaultFactory implements Factory {
        @Override
        @SuppressWarnings("unchecked")
        public <T> T create(Class<T> cls) throws Exception {
            try {
                CommandModel model = CommandModelRegistry.getModel(cls);
                if (model != null) {
                    return (T) model.createInstance();
                }
            } catch (IllegalStateException e) {
                // Not a @Command class — fall through to instance creator lookup
            }
            // No CommandModel — this is a mixin or other non-@Command class.
            // Use the build-time generated instance creator.
            Supplier<Object> creator = CommandModelRegistry.getInstanceCreator(cls);
            if (creator != null) {
                return (T) creator.get();
            }
            throw new IllegalStateException(
                    "No build-time generated instance creator for " + cls.getName()
                            + ". Ensure the class is discovered during the Quarkus build.");
        }
    }
}
