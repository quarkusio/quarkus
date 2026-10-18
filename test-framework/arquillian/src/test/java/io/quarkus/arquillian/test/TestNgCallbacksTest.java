package io.quarkus.arquillian.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;

/**
 * Verifies that the TestNG class level callbacks are invoked on the in container test instance, by running
 * {@link ClassCallbacksTestNg} through TestNG itself. The build runs the JUnit provider only, so a TestNG test which is
 * named as one would be silently skipped instead.
 */
public class TestNgCallbacksTest {

    @Test
    public void testClassLevelCallbacksAreInvokedInContainer() {
        System.clearProperty(ClassCallbacksTestNg.AFTER_CLASS_CLASS_LOADERS);

        run(ClassCallbacksTestNg.class);

        String classLoaders = System.getProperty(ClassCallbacksTestNg.AFTER_CLASS_CLASS_LOADERS, "");
        assertTrue(classLoaders.contains(QuarkusClassLoader.class.getSimpleName()),
                "@AfterClass was not invoked on the in container test instance, but only by " + classLoaders);
    }

    @Test
    public void testMethodLevelCallbacksAreNotInvokedTwiceWhenRunningAsClient() {
        System.clearProperty(MethodCallbacksRunAsClientTestNg.BEFORE_METHOD_COUNT);

        run(MethodCallbacksRunAsClientTestNg.class);

        assertEquals(1, MethodCallbacksRunAsClientTestNg.count());
    }

    private static void run(Class<?> testClass) {
        TestListenerAdapter listener = new TestListenerAdapter();
        TestNG testng = new TestNG();
        testng.setTestClasses(new Class[] { testClass });
        testng.addListener(listener);
        testng.setUseDefaultListeners(false);
        testng.run();

        assertEquals(List.of(), listener.getConfigurationFailures().stream().map(r -> r.getThrowable().toString()).toList());
        assertEquals(List.of(), listener.getFailedTests().stream().map(r -> r.getThrowable().toString()).toList());
        assertEquals(1, listener.getPassedTests().size());
    }
}
