package io.quarkus.core.deployment.action.impl;

import java.io.IOException;

import io.smallrye.serial.Serialized;
import io.smallrye.serial.spi.ObjectSerializer;

/**
 * A custom {@link ObjectSerializer} for {@code smallrye-serial} that intercepts
 * {@code @ConfigMapping} + {@code @ConfigRoot} config instances, producing a
 * {@link SerializedConfigMapping} instead of attempting standard serialization.
 * <p>
 * Config mapping instances are SmallRye Config proxies that are not serializable.
 * At runtime, they are resolved directly from SmallRye Config via
 * {@code ConfigLookup.getConfigMapping(type)}.
 * <p>
 * Registered at a priority above {@code PRIORITY_REPLACE} to intercept before
 * any default serializer attempts to serialize the config proxy.
 */
final class ConfigMappingSerializer implements ObjectSerializer {

    /**
     * Singleton instance.
     */
    static final ConfigMappingSerializer INSTANCE = new ConfigMappingSerializer();

    private ConfigMappingSerializer() {
    }

    @Override
    public Serialized serialize(Context ctxt, Object object) throws IOException {
        Class<?> configInterface = ConfigMappingDetector.findConfigMappingInterface(object.getClass());
        if (configInterface != null && ConfigMappingDetector.findConfigRoot(object.getClass()) != null) {
            return new SerializedConfigMapping(configInterface.describeConstable().orElseThrow());
        }
        return ctxt.next();
    }

    @Override
    public int priority() {
        return PRIORITY_REPLACE + 2;
    }
}
