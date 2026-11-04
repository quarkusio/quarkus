package io.quarkus.core.deployment.service.impl;

import java.lang.constant.ClassDesc;

import io.smallrye.serial.Serialized;

/**
 * A {@link Serialized} representation of a {@code @ConfigMapping} + {@code @ConfigRoot}
 * config instance captured by a service action lambda.
 * <p>
 * Produced by {@link ConfigMappingSerializer}. The emitter loads the config mapping
 * directly from SmallRye Config at runtime via {@code ConfigLookup.getConfigMapping(type, prefix)}.
 */
public final class SerializedConfigMapping extends Serialized {
    private final ClassDesc configInterface;
    private final String prefix;

    /**
     * Construct a new instance.
     *
     * @param configInterface the class descriptor of the {@code @ConfigMapping} interface
     * @param prefix the prefix the mapping is registered with
     */
    public SerializedConfigMapping(ClassDesc configInterface, String prefix) {
        this.configInterface = configInterface;
        this.prefix = prefix;
    }

    /**
     * {@return the class descriptor of the config mapping interface}
     */
    public ClassDesc configInterface() {
        return configInterface;
    }

    /**
     * {@return the prefix the config mapping is registered with}
     */
    public String prefix() {
        return prefix;
    }
}
