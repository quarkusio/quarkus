package io.quarkus.core.deployment.action.impl;

import java.lang.constant.ClassDesc;

import io.smallrye.serial.Serialized;

/**
 * A {@link Serialized} representation of a {@code @ConfigMapping} + {@code @ConfigRoot}
 * config instance captured by a service action lambda.
 * <p>
 * Produced by {@link ConfigMappingSerializer}. The emitter loads the config mapping
 * directly from SmallRye Config at runtime via {@code ConfigLookup.getConfigMapping(type)}.
 */
public final class SerializedConfigMapping extends Serialized {
    private final ClassDesc configInterface;

    /**
     * Construct a new instance.
     *
     * @param configInterface the class descriptor of the {@code @ConfigMapping} interface
     */
    public SerializedConfigMapping(ClassDesc configInterface) {
        this.configInterface = configInterface;
    }

    /**
     * {@return the class descriptor of the config mapping interface}
     */
    public ClassDesc configInterface() {
        return configInterface;
    }
}
