package io.quarkus.core.deployment.action.impl;

import io.smallrye.common.constraint.Assert;
import io.smallrye.serial.Serialized;

/**
 * A serialized (captured) Quarkus class loader, which can be rematerialized
 * in the target as the defining class loader of the generated class.
 */
public final class SerializedQuarkusClassLoader extends Serialized {
    private static final SerializedQuarkusClassLoader RUNTIME = new SerializedQuarkusClassLoader(Kind.RUNTIME);

    private final Kind kind;

    private SerializedQuarkusClassLoader(final Kind kind) {
        this.kind = Assert.checkNotNullParam("kind", kind);
    }

    /**
     * {@return the serialized class loader of the given kind}
     *
     * @param kind the class loader kind (must not be {@code null})
     */
    public static Serialized of(final Kind kind) {
        Assert.checkNotNullParam("kind", kind);
        return switch (kind) {
            case RUNTIME -> RUNTIME;
        };
    }

    /**
     * {@return the class loader kind (not {@code null})}
     */
    public Kind kind() {
        return kind;
    }

    /**
     * The enumeration of possible class loader kinds.
     */
    public enum Kind {
        /**
         * The kind of the base run time class loader.
         */
        RUNTIME,
    }
}
