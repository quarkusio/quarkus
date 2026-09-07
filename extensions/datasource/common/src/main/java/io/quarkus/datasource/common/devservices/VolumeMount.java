package io.quarkus.datasource.common.devservices;

import java.util.Objects;

/**
 * A volume mount to be applied to a Dev Service container.
 * <p>
 * This is the structured equivalent of the legacy {@code host-path=container-path} map form,
 * allowing the mount type and access mode to be expressed explicitly.
 */
public record VolumeMount(Type type, String source, String target, boolean readOnly) {

    public enum Type {
        /**
         * Bind mount of a host file system path.
         */
        BIND,
        /**
         * Mount of a resource resolved from the application classpath.
         */
        CLASSPATH
    }

    public VolumeMount {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(target, "target must not be null");
    }

    public static VolumeMount bind(String source, String target, boolean readOnly) {
        return new VolumeMount(Type.BIND, source, target, readOnly);
    }

    public static VolumeMount classpath(String source, String target, boolean readOnly) {
        return new VolumeMount(Type.CLASSPATH, source, target, readOnly);
    }
}
