package io.quarkus.mongodb.panache.deployment;

public interface TypeBundle {
    ByteCodeType entity();

    ByteCodeType entityBase();

    ByteCodeType entityBaseCompanion();

    ByteCodeType entityCompanion();

    ByteCodeType entityCompanionBase();

    ByteCodeType operations();

    ByteCodeType queryType();

    ByteCodeType repository();

    ByteCodeType repositoryBase();

    ByteCodeType updateType();
}
