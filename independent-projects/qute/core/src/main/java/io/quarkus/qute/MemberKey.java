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
    final int hashCode;

    MemberKey(Class<?> clazz, String name, int numberOfParams) {
        this.clazz = clazz;
        this.name = name;
        this.numberOfParams = numberOfParams;
        final int prime = 31;
        int result = 1;
        result = prime * result + clazz.hashCode();
        result = prime * result + name.hashCode();
        result = prime * result + numberOfParams;
        this.hashCode = result;
    }

    @Override
    public int hashCode() {
        return hashCode;
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

    static MemberKey from(EvalContext context) {
        Object contextObject = context.getBase();
        String name = context.getName();
        Class<?> baseClass = contextObject.getClass();
        if (contextObject instanceof Class<?>) {
            Class<?> clazz = (Class<?>) contextObject;
            if (clazz.isEnum() && ("values".equals(name) || isConstantName(clazz, name))) {
                // Special handling for enums - allows to access values() and constants
                baseClass = clazz;
            }
        }
        return new MemberKey(baseClass, name, context.getParams().size());
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
