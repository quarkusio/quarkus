package io.quarkus.liquibase.runtime.graal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

/**
 * The liquibase service class loader for native image.
 *
 * The liquibase has own implementation of the class-path scanner to load the Services (Interface implementation).
 * For this we generated txt files (list of implementation classes) at build time which are used in the native run time.
 */
public class LiquibaseServiceLoader {

    private static final Logger LOGGER = Logger.getLogger(LiquibaseServiceLoader.class);

    /**
     * File prefix with the service implementation classes list. It is generated dynamically in the Liquibase Quarkus Processor
     */
    private final static String SERVICES_IMPL = "META-INF/liquibase/";

    /**
     * The service implementation classes list resource name
     * 
     * @param requiredInterface the required interface
     * @return the resource file in the class-path
     */
    public static String serviceResourceFile(Class<?> requiredInterface) {
        return SERVICES_IMPL + requiredInterface.getName() + ".txt";
    }

    /**
     * Finds all classes for the required interface. The list of classes will be load from the txt files
     * which are generated in the build phase.
     *
     * @param requiredInterface the required interface
     * @return the list of the classes that implements the required interface
     */
    public static List<Class<?>> findClassesImpl(Class<?> requiredInterface) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String resourceName = serviceResourceFile(requiredInterface);
        LOGGER.debug("Liquibase service resource file: " + resourceName);
        try (InputStream resource = classLoader.getResourceAsStream(resourceName);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(Objects.requireNonNull(resource), StandardCharsets.UTF_8))) {

            return reader.lines().map(className -> {
                try {
                    LOGGER.debug("Loading liquibase class: " + className);
                    return Class.forName(className);
                } catch (ClassNotFoundException ex) {
                    throw new IllegalStateException(ex);
                }
            }).collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

    }
}
