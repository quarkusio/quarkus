package io.quarkus.hibernate.orm.deployment.vector;

import java.util.function.BooleanSupplier;

/**
 * Supplier that can be used to only run build steps
 * if Hibernate Vector is available in the classpath.
 */
public class HibernateVectorAvailable implements BooleanSupplier {
    static final String HIBERNATE_VECTOR_SERVICE_CLASS = "org.hibernate.vector.SparseFloatVector";
    static final boolean HIBERNATE_VECTOR_AVAILABLE = isClassAvailable(
            HIBERNATE_VECTOR_SERVICE_CLASS);

    @Override
    public boolean getAsBoolean() {
        return HIBERNATE_VECTOR_AVAILABLE;
    }

    private static boolean isClassAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
