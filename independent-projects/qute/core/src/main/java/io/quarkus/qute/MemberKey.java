package io.quarkus.qute;

/**
 *
 * @author Martin Kouba
 */
final class MemberKey {

    private final Class<?> clazz;

    private final String name;

    public Class<?> getClazz() {
        return clazz;
    }

    public String getName() {
        return name;
    }

    MemberKey(Class<?> clazz, String name) {
        this.clazz = clazz;
        this.name = name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof MemberKey))
            return false;
        MemberKey other = (MemberKey) obj;
        if (clazz == null) {
            if (other.clazz != null)
                return false;
        } else if (!clazz.equals(other.clazz))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("MemberKey [clazz: %s, name: %s]", clazz, name);
    }

    static MemberKey newInstance(Object contextObject, String name) {
        if (contextObject instanceof Class<?>) {
            Class<?> clazz = (Class<?>) contextObject;
            if (clazz.isEnum() && ("values".equals(name) || isConstantName(clazz, name))) {
                // Special handling for enums - allows to access values() and constants
                return new MemberKey(clazz, name);
            }
        }
        return new MemberKey(contextObject.getClass(), name);
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
