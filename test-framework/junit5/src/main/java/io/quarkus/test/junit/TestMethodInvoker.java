package io.quarkus.test.junit;

import java.lang.reflect.Method;
import java.util.List;

/**
 * An SPI that allows modules that integrate with {@link QuarkusTestExtension} to alter the invocation of the actual test
 * method.
 *
 * Implementations are loaded from the same ClassLoader that loads the actual test method
 * (so NOT the ClassLoader that loads the QuarkusTestExtension) which results in {@link QuarkusTestExtension}
 * having to invoke the method with reflection, but otherwise allows the implementation to (mostly) avoid
 * dealing with class-loading.
 */
public interface TestMethodInvoker {

    /**
     * Determine whether this method invoker handles a test method parameter on its own
     */
    default boolean handlesMethodParamType(String paramClassName) {
        return false;
    }

    default Object methodParamInstance(String paramClassName) {
        throw new IllegalStateException("Should never be called");
    }

    /**
     * Determines whether this SPI should handle the test method.
     * The class and method are those supplied by JUnit which means they have been loaded from the original ClassLoader
     * that loaded the QuarkusTestExtension and NOT the ClassLoader that loaded this class.
     */
    boolean supportsMethod(Class<?> originalTestClass, Method originalTestMethod);

    /**
     * Invoked by QuarkusTestExtension when the test method needs to actually be run.
     * The parameters being passed have been loaded by QuarkusTestExtension on the proper ClassLoader
     */
    Object invoke(Object actualTestInstance, Method actualTestMethod, List<Object> actualTestMethodArgs, String testClassName)
            throws Throwable;
}
