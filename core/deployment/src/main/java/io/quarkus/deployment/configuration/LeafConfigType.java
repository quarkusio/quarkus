package io.quarkus.deployment.configuration;

import java.lang.reflect.Field;

import org.jboss.protean.gizmo.BytecodeCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.wildfly.common.annotation.NotNull;

import io.quarkus.runtime.configuration.NameIterator;
import io.smallrye.config.SmallRyeConfig;

/**
 * A node which contains a regular value. Leaf nodes can never be directly acquired.
 */
public abstract class LeafConfigType extends ConfigType {

    LeafConfigType(final String containingName, final CompoundConfigType container, final boolean consumeSegment) {
        super(containingName, container, consumeSegment);
    }

    public void load() {
    }

    /**
     * Get the class of the individual item. This is the unwrapped type of {@code Optional}, {@code Collection}, etc.
     *
     * @return the item class (must not be {@code null})
     */
    public abstract Class<?> getItemClass();

    /**
     * Handle a configuration key from the input file.
     * 
     * @param name the configuration property name
     * @param config the source configuration
     */
    public abstract void acceptConfigurationValue(@NotNull NameIterator name, @NotNull SmallRyeConfig config);

    public abstract void generateAcceptConfigurationValue(BytecodeCreator body, ResultHandle name, ResultHandle config);

    abstract void acceptConfigurationValueIntoGroup(Object enclosing, Field field, NameIterator name, SmallRyeConfig config);

    abstract void generateAcceptConfigurationValueIntoGroup(BytecodeCreator body, ResultHandle enclosing,
            final MethodDescriptor setter, ResultHandle name, ResultHandle config);
}
