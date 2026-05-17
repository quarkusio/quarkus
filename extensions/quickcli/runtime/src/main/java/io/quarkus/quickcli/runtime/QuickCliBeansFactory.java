package io.quarkus.quickcli.runtime;

import java.util.function.Supplier;

import jakarta.enterprise.inject.Any;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.quickcli.CommandLine;
import io.quarkus.quickcli.model.CommandModel;
import io.quarkus.quickcli.model.CommandModelRegistry;

/**
 * An {@link CommandLine.Factory} that first tries to resolve instances from the Quarkus Arc
 * CDI container, falling back to the generated {@link CommandModel#createInstance()}
 * which uses a direct {@code new} call. No reflection is used.
 */
class QuickCliBeansFactory implements CommandLine.Factory {

    @Override
    @SuppressWarnings("unchecked")
    public <K> K create(Class<K> aClass) throws Exception {
        // Use @Any qualifier to match beans regardless of their qualifier (e.g. @TopCommand)
        InstanceHandle<K> instance = Arc.container().instance(aClass, Any.Literal.INSTANCE);
        if (instance.isAvailable()) {
            return instance.get();
        }
        // Try CommandModel for @Command classes
        try {
            return (K) CommandModelRegistry.getModel(aClass).createInstance();
        } catch (IllegalStateException ignored) {
            // Not a @Command class — try instance creator for mixins/arg groups
        }
        // Try build-time generated instance creator for mixin/arg group types
        Supplier<Object> creator = CommandModelRegistry.getInstanceCreator(aClass);
        if (creator != null) {
            return (K) creator.get();
        }
        throw new IllegalArgumentException(
                "No CDI bean or CommandModel found for " + aClass.getName());
    }
}
