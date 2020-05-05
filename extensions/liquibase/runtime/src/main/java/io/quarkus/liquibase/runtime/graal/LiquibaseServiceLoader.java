package io.quarkus.liquibase.runtime.graal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The liquibase service class loader for native image.
 *
 * The liquibase extension has its own implementation of the class-path scanner to load the Services (Interface implementation).
 */
public class LiquibaseServiceLoader {

    private static Map<String, List<String>> servicesImplementations;

    /**
     * Finds all classes for the required interface.
     *
     * @param requiredInterface the required interface
     * @return the list of the classes that implements the required interface
     */
    public static List<Class<?>> findClassesImpl(Class<?> requiredInterface) {
        List<String> classesImplementationNames = servicesImplementations.get(requiredInterface.getName());

        if (classesImplementationNames == null || classesImplementationNames.isEmpty()) {
            return Collections.emptyList();
        }

        List<Class<?>> classImplementations = new ArrayList<>();

        for (String classImplementation : classesImplementationNames) {
            try {
                classImplementations.add(Class.forName(classImplementation));
            } catch (ClassNotFoundException exception) {
                throw new IllegalStateException(exception);
            }
        }

        return classImplementations;
    }

    public static void setServicesImplementations(Map<String, List<String>> servicesImplementations) {
        LiquibaseServiceLoader.servicesImplementations = servicesImplementations;
    }
}
