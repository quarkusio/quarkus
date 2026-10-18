package io.quarkus.hibernate.reactive.deployment;

import org.junit.jupiter.api.Test;

public final class PersisterNamesTest {

    @Test
    public void testClassesExist() {
        for (String clazz : HibernateReactiveProcessor.REFLECTIVE_CONSTRUCTORS_NEEDED) {
            assertClassExists(clazz);
        }
    }

    private void assertClassExists(String clazz) {
        try {
            Thread.currentThread().getContextClassLoader().loadClass(clazz);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
