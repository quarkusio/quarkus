package io.quarkus.quickcli.model;

/**
 * Sets a value on a command (or mixin/arggroup) instance field.
 * Implementations are generated at build time with direct field access
 * or setter calls — no reflection is used at runtime.
 */
@FunctionalInterface
public interface FieldAccessor {

    void set(Object instance, Object value);
}
