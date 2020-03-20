package io.quarkus.test.junit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.junit.jupiter.api.function.Executable;

/**
 * This class needs to be used when users want to use JUnit 5's {@link org.junit.jupiter.api.DynamicTest} along with
 * {@link org.junit.jupiter.api.TestFactory} in a {@link QuarkusTest}.
 *
 *
 * An example usage in a test is:
 *
 * <code><pre>
 * &#64;TestFactory
 * public List<?> dynamicTests() {
 *   return Arrays.asList(
 *     QuarkusDynamicTest.dynamicTest("test 1", () -> {
 *       assertEquals(1, 1);
 *       // of course more complex things can be done here, like accessing a field injected with @Inject
 *     }),
 *     QuarkusDynamicTest.dynamicTest("test 2", () -> {
 *       assertEquals(2, 2);
 *     })
 *   );
 * }
 * </pre></code>
 */
public class QuarkusDynamicTest {

    private static ClassLoader cl;
    private static Method dynamicTestMethod;
    private static Constructor<?> quarkusExecutableClassConstructor;

    public static Object dynamicTest(String displayName, Executable executable) {
        try {
            Object executableInstance = getQuarkusExecutableClassConstructor().newInstance(executable);
            return getDynamicTestMethod().invoke(null, displayName, executableInstance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void setClassLoader(ClassLoader classLoader) {
        cl = classLoader;
    }

    private static Method getDynamicTestMethod() throws ClassNotFoundException, NoSuchMethodException {
        if (dynamicTestMethod == null) {
            dynamicTestMethod = Class.forName("org.junit.jupiter.api.DynamicTest", false, cl)
                    .getMethod("dynamicTest", String.class, Class
                            .forName("org.junit.jupiter.api.function.Executable", false, cl));
        }
        return dynamicTestMethod;
    }

    private static Constructor<?> getQuarkusExecutableClassConstructor() throws ClassNotFoundException, NoSuchMethodException {
        if (quarkusExecutableClassConstructor == null) {
            quarkusExecutableClassConstructor = Class.forName("io.quarkus.test.junit.QuarkusExecutable", false, cl)
                    .getConstructor(Object.class);
        }
        return quarkusExecutableClassConstructor;
    }

}
