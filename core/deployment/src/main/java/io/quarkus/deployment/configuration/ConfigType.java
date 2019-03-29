package io.quarkus.deployment.configuration;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;

import io.quarkus.deployment.AccessorFinder;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.configuration.ExpandingConfigSource;
import io.quarkus.runtime.configuration.NameIterator;
import io.smallrye.config.SmallRyeConfig;

/**
 */
public abstract class ConfigType {
    static final MethodDescriptor NI_PREV_METHOD = MethodDescriptor.ofMethod(NameIterator.class, "previous", void.class);
    static final MethodDescriptor NI_NEXT_METHOD = MethodDescriptor.ofMethod(NameIterator.class, "next", void.class);
    static final MethodDescriptor NI_HAS_NEXT_METHOD = MethodDescriptor.ofMethod(NameIterator.class, "hasNext", boolean.class);
    static final MethodDescriptor NI_GET_NEXT_SEGMENT = MethodDescriptor.ofMethod(NameIterator.class, "getNextSegment",
            String.class);

    static final MethodDescriptor SRC_CONVERT_METHOD = MethodDescriptor.ofMethod(SmallRyeConfig.class, "convert", Object.class,
            String.class, Class.class);
    static final MethodDescriptor SRC_GET_OPT_METHOD = MethodDescriptor.ofMethod(SmallRyeConfig.class, "getOptionalValue",
            Optional.class, String.class, Class.class);
    static final MethodDescriptor SRC_GET_VALUE = MethodDescriptor.ofMethod(SmallRyeConfig.class, "getValue", Object.class,
            String.class, Class.class);
    static final MethodDescriptor SRC_GET_VALUES_METHOD = MethodDescriptor.ofMethod(SmallRyeConfig.class, "getValues",
            Collection.class, String.class, Class.class, IntFunction.class);

    static final MethodDescriptor OBJ_TO_STRING_METHOD = MethodDescriptor.ofMethod(Object.class, "toString", String.class);

    static final MethodDescriptor OPT_OR_ELSE_METHOD = MethodDescriptor.ofMethod(Optional.class, "orElse", Object.class,
            Object.class);
    static final MethodDescriptor OPT_OF_NULLABLE_METHOD = MethodDescriptor.ofMethod(Optional.class, "ofNullable",
            Optional.class, Object.class);
    static final MethodDescriptor OPT_EMPTY_METHOD = MethodDescriptor.ofMethod(Optional.class, "empty", Optional.class);

    static final MethodDescriptor MAP_PUT_METHOD = MethodDescriptor.ofMethod(Map.class, "put", Object.class, Object.class,
            Object.class);

    static final MethodDescriptor ECS_CACHE_CTOR = MethodDescriptor.ofConstructor(ExpandingConfigSource.Cache.class);

    /**
     * Containing name. This is a field name or a map key, <em>not</em> a configuration key segment; as such, it is
     * never {@code null} unless the containing name is intentionally dynamic.
     */
    private final String containingName;
    /**
     * The containing node, or {@code null} if the node is a root.
     */
    private final CompoundConfigType container;
    /**
     * Consume a segment of the name when traversing this node. Always {@code true} if the containing name is dynamic,
     * otherwise only {@code true} if the node is a configuration group node with an empty relative name.
     */
    private final boolean consumeSegment;

    ConfigType(final String containingName, final CompoundConfigType container, final boolean consumeSegment) {
        this.containingName = containingName;
        this.container = container;
        this.consumeSegment = consumeSegment;
    }

    static IllegalAccessError toError(final IllegalAccessException e) {
        IllegalAccessError e2 = new IllegalAccessError(e.getMessage());
        e2.setStackTrace(e.getStackTrace());
        return e2;
    }

    static InstantiationError toError(final InstantiationException e) {
        InstantiationError e2 = new InstantiationError(e.getMessage());
        e2.setStackTrace(e.getStackTrace());
        return e2;
    }

    public String getContainingName() {
        return containingName;
    }

    public CompoundConfigType getContainer() {
        return container;
    }

    public <T extends CompoundConfigType> T getContainer(Class<T> expect) {
        final CompoundConfigType container = getContainer();
        if (expect.isInstance(container))
            return expect.cast(container);
        throw new IllegalStateException(
                "Container is not a supported type; expected " + expect + " but got " + container.getClass());
    }

    public boolean isConsumeSegment() {
        return consumeSegment;
    }

    /**
     * Load all configuration classes to enable configuration to be instantiated.
     *
     * @throws ClassNotFoundException if a required class was not found
     */
    public abstract void load() throws ClassNotFoundException;

    /**
     * A reusable method which returns an exception that can be thrown when a configuration
     * node is used without its class being loaded.
     *
     * @return the not-loaded exception
     */
    protected static IllegalStateException notLoadedException() {
        return new IllegalStateException("Configuration tree classes not loaded");
    }

    /**
     * Get the default value of this type into the enclosing element.
     * 
     * @param enclosing the instance of the enclosing type (must not be {@code null})
     * @param cache
     * @param config the configuration (must not be {@code null})
     * @param field the field to read the value into
     */
    abstract void getDefaultValueIntoEnclosingGroup(final Object enclosing, final ExpandingConfigSource.Cache cache,
            final SmallRyeConfig config, final Field field);

    abstract void generateGetDefaultValueIntoEnclosingGroup(final BytecodeCreator body, final ResultHandle enclosing,
            final MethodDescriptor setter, final ResultHandle cache, final ResultHandle config);

    public abstract ResultHandle writeInitialization(final BytecodeCreator body, final AccessorFinder accessorFinder,
            final ResultHandle cache, final ResultHandle smallRyeConfig);

    public ConfigDefinition getConfigDefinition() {
        return container.getConfigDefinition();
    }
}
