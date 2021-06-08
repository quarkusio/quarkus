package io.quarkus.test.junit;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * An SPI that allows modules that integrate with QuarkusTestExtension to alter the invocation of the actual test method.
 *
 * Implementations are loaded from the ClassLoader that loads the QuarkusTest extension, but they need to take care
 * that the actual test method is invoked on a specific QuarkusClassLoader (which is provided in the {@code init} method).
 */
public interface TestMethodInvoker {

    /**
     * Invoked by QuarkusTestExtension with ClassLoader that is used to load the actual test instance
     */
    void init(ClassLoader actualTestCL);

    /**
     * Allows this SPI to handle method parameters not directly handled by QuarkusTestExtension
     */
    default CustomMethodTypesHandler customMethodTypesHandler() {
        return CustomMethodTypesHandler.Default.INSTANCE;
    }

    /**
     * Determines whether this SPI should handle the test method.
     * The class and method are those supplied by JUnit which means they have been loaded from the original ClassLoader
     * that loaded the QuarkusTestExtension.
     */
    boolean supportsMethod(Class<?> originalTestClass, Method originalTestMethod);

    /**
     * Invoked by QuarkusTestExtension when the test method needs to actually be run.
     * The parameters being passed have been loaded by QuarkusTestExtension on the proper ClassLoader
     */
    Object invoke(Object actualTestInstance, Method actualTestMethod, List<Object> actualTestMethodArgs, String testClassName)
            throws Throwable;

    interface CustomMethodTypesHandler {

        /**
         * Returns the classes (loaded from the original test ClassLoader) that are handled by this type
         */
        List<Class<?>> handledTypes();

        /**
         * Creates instances of the handled types. The instance of the class MUST be loaded from the supplied ClassLoader
         * (which is the actual test ClassLoader)
         */
        <T> T instance(Class<?> clazz, ClassLoader actualTestClassLoader);

        final class Default implements CustomMethodTypesHandler {

            private static final Default INSTANCE = new Default();

            @Override
            public List<Class<?>> handledTypes() {
                return Collections.emptyList();
            }

            @Override
            public <T> T instance(Class<?> clazz, ClassLoader actualTestClassLoader) {
                throw new IllegalStateException("Should not have been called");
            }
        }
    }
}
