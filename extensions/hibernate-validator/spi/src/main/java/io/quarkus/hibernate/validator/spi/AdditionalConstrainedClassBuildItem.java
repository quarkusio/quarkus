package io.quarkus.hibernate.validator.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class AdditionalConstrainedClassBuildItem extends MultiBuildItem {

    private static final byte[] EMPTY = new byte[0];

    private final Class<?> clazz;
    private final String name;
    private final byte[] bytes;

    private AdditionalConstrainedClassBuildItem(Class<?> clazz) {
        this.clazz = clazz;
        this.name = clazz.getName();
        this.bytes = EMPTY;
    }

    private AdditionalConstrainedClassBuildItem(String name, byte[] bytes) {
        this.clazz = null;
        this.name = name;
        this.bytes = bytes;
    }

    public static AdditionalConstrainedClassBuildItem of(Class<?> clazz) {
        return new AdditionalConstrainedClassBuildItem(clazz);
    }

    public static AdditionalConstrainedClassBuildItem of(String name, byte[] bytes) {
        return new AdditionalConstrainedClassBuildItem(name, bytes);
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public String getName() {
        return name;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public boolean isGenerated() {
        return clazz == null;
    }
}
