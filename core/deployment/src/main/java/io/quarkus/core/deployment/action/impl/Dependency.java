package io.quarkus.core.deployment.action.impl;

import java.util.List;

import io.quarkus.runtime.StartupContext;
import io.smallrye.common.constraint.Assert;

/**
 * A service dependency declaration.
 *
 * @param type the dependency type
 * @param nameParts the parts of the name (must not be {@code null})
 * @param flags bitset of dependency flags (use the {@code FL_*} constants)
 */
public record Dependency(Class<?> type, List<String> nameParts, int flags) {

    /** The dependency value is injected into the action (not just ordering). */
    public static final int FL_INJECTED = 1;

    /** The dependency is optional (absent providers do not cause validation errors). */
    public static final int FL_OPTIONAL = 2;

    /** The dependency consumes all services of the given type as a {@code Map<String, T>}. */
    public static final int FL_CONSUME_ALL = 4;

    /**
     * The dependency is a {@code @ConfigRoot} config mapping resolved directly
     * from SmallRye Config at runtime, bypassing the service values map.
     */
    public static final int FL_CONFIG_DIRECT = 8;

    public Dependency {
        Assert.checkNotNullParam("type", type);
        nameParts = List.copyOf(Assert.checkNotNullParam("nameParts", nameParts));
        for (String namePart : nameParts) {
            if (namePart.indexOf('/') != -1) {
                throw new IllegalArgumentException("Name part must not contain '/'");
            }
        }
    }

    /**
     * {@return {@code true} if this dependency is injected into the action}
     */
    public boolean injected() {
        return (flags & FL_INJECTED) != 0;
    }

    /**
     * {@return {@code true} if this dependency is optional}
     */
    public boolean optional() {
        return (flags & FL_OPTIONAL) != 0;
    }

    /**
     * {@return {@code true} if this dependency consumes all matching services as a map}
     */
    public boolean consumeAll() {
        return (flags & FL_CONSUME_ALL) != 0;
    }

    /**
     * {@return {@code true} if this is a config-direct dependency}
     */
    public boolean configDirect() {
        return (flags & FL_CONFIG_DIRECT) != 0;
    }

    /**
     * Get the key used to look up this dependency's value in the {@link StartupContext}.
     * This will only be used until bytecode recording has been removed.
     *
     * @return the key string
     */
    public String key() {
        return LambdaTransliterator.serviceKey(type, nameParts);
    }

    /**
     * Get the key prefix for matching services in a {@code consumeAll} dependency.
     * For a type {@code Foo.class} with no name parts, this returns {@code "com.example.Foo:"}.
     * A service key matches if it starts with this prefix.
     *
     * @return the key prefix string
     */
    public String keyPrefix() {
        return type.getName() + ":";
    }
}
