package io.quarkus.hibernate.orm.runtime;

public class PersistenceUnitUtil {

    public static final String DEFAULT_PERSISTENCE_UNIT_NAME = "<default>";

    public static boolean isDefaultPersistenceUnit(String name) {
        return DEFAULT_PERSISTENCE_UNIT_NAME.equals(name);
    }
}
