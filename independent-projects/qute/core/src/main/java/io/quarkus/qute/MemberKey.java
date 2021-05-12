package io.quarkus.qute;

import java.util.Objects;

/**
 *
 * @see ReflectionValueResolver
 */
final class MemberKey {

    final Class<?> clazz;
    final String name;
    final int numberOfParams;

    MemberKey(Class<?> clazz, String name, int numberOfParams) {
        this.clazz = clazz;
        this.name = name;
        this.numberOfParams = numberOfParams;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + clazz.hashCode();
        result = prime * result + name.hashCode();
        result = prime * result + numberOfParams;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MemberKey other = (MemberKey) obj;
        return Objects.equals(clazz, other.clazz) && Objects.equals(name, other.name) && numberOfParams == other.numberOfParams;
    }

    @Override
    public String toString() {
        return String.format("MemberKey [clazz: %s, name: %s, numberOfParams: %s]", clazz, name, numberOfParams);
    }

    static MemberKey from(Object contextObject, String name, int numberOfParams) {
        if (contextObject instanceof Class<?>) {
            Class<?> clazz = (Class<?>) contextObject;
            if (clazz.isEnum() && ("values".equals(name) || isConstantName(clazz, name))) {
                // Special handling for enums - allows to access values() and constants
                return new MemberKey(clazz, name, numberOfParams);
            }
        }
        return new MemberKey(contextObject.getClass(), name, numberOfParams);
    }

    private static boolean isConstantName(Class<?> enumClazz, String name) {
        for (Object constant : enumClazz.getEnumConstants()) {
            if (name.equals(constant.toString())) {
                return true;
            }
        }
        return false;
    }

}
