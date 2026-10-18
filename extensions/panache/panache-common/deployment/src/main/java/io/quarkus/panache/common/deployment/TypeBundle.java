package io.quarkus.panache.common.deployment;

public interface TypeBundle {
    ByteCodeType entity();

    ByteCodeType entityBase();

    default ByteCodeType entityCompanion() {
        throw new UnsupportedOperationException("Companions are not supported in Java.");
    }

    default ByteCodeType entityCompanionBase() {
        throw new UnsupportedOperationException("Companions are not supported in Java.");
    }

    ByteCodeType operations();

    ByteCodeType queryType();

    ByteCodeType repository();

    ByteCodeType repositoryBase();

    default ByteCodeType updateType() {
        throw new UnsupportedOperationException("Update types are only supported in MongoDB contexts");
    };
}
