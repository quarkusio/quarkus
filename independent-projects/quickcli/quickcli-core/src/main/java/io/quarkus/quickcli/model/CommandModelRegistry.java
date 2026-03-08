package io.quarkus.quickcli.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Registry of command models. Generated model classes self-register via a static
 * initializer when their class is loaded. Loading is triggered by a {@link Class#forName}
 * call using a deterministic naming convention ({@code ClassName_QuickCliModel}).
 *
 * <p>No ServiceLoader, no classpath scanning, no annotation reflection.</p>
 */
public final class CommandModelRegistry {

    private static final Map<Class<?>, CommandModel> MODELS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Supplier<Object>> INSTANCE_CREATORS = new ConcurrentHashMap<>();

    private CommandModelRegistry() {
    }

    /**
     * Get the command model for the given command class. If the model is not yet
     * loaded, attempts to load the generated model class by naming convention
     * using {@link Class#forName}.
     *
     * @param commandClass the @Command annotated class
     * @return the pre-generated model
     * @throws IllegalStateException if no generated model is found
     */
    public static CommandModel getModel(Class<?> commandClass) {
        CommandModel model = MODELS.get(commandClass);
        if (model != null) {
            return model;
        }

        // Try to load the generated model class by naming convention.
        // The class name is deterministic: <commandFQCN>_QuickCliModel.
        // Class.forName triggers the static initializer which calls register().
        String modelClassName = commandClass.getName() + "_QuickCliModel";
        try {
            Class.forName(modelClassName, true, commandClass.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "No build-time generated model found for " + commandClass.getName()
                            + ". Ensure the class is annotated with @Command and processed"
                            + " by the QuickCLI annotation processor or Quarkus build step.");
        }

        model = MODELS.get(commandClass);
        if (model == null) {
            throw new IllegalStateException(
                    "Model class " + modelClassName + " was loaded but failed to register."
                            + " Check for errors in its static initializer.");
        }
        return model;
    }

    /**
     * Register a command model. Called from the static initializer of each
     * generated model class.
     */
    public static void register(CommandModel model) {
        MODELS.put(model.commandClass(), model);
    }

    /**
     * Register an instance creator for a non-@Command class (e.g. mixin, arg group).
     * Called from the static initializer of generated model classes.
     */
    public static void registerInstanceCreator(Class<?> cls, Supplier<Object> creator) {
        INSTANCE_CREATORS.put(cls, creator);
    }

    /**
     * Get a build-time generated instance creator for the given class.
     *
     * @return the supplier, or null if none was registered
     */
    public static Supplier<Object> getInstanceCreator(Class<?> cls) {
        return INSTANCE_CREATORS.get(cls);
    }
}
