package io.quarkus.arquillian;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jboss.arquillian.core.api.annotation.Observes;

public class QuarkusBeforeAfterLifecycle {

    private static final String JUNIT_CALLBACKS = "io.quarkus.arquillian.QuarkusJunitCallbacks";
    private static final String JUNIT_INVOKE_BEFORES = "invokeBefores";
    private static final String JUNIT_INVOKE_AFTERS = "invokeAfters";

    public void on(@Observes(precedence = -100) org.jboss.arquillian.test.spi.event.suite.Before event) throws Throwable {
        if (isJunitAvailable()) {
            invokeJunitCallbacks(JUNIT_INVOKE_BEFORES);
        }
    }

    public void on(@Observes(precedence = 100) org.jboss.arquillian.test.spi.event.suite.After event) throws Throwable {
        if (isJunitAvailable()) {
            invokeJunitCallbacks(JUNIT_INVOKE_AFTERS);
        }
    }

    private boolean isJunitAvailable() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            cl.loadClass("org.jboss.arquillian.junit.container.JUnitTestRunner");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void invokeJunitCallbacks(String methodName)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            NoSuchMethodException, SecurityException, ClassNotFoundException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Class<?> callbacksClass = cl.loadClass(JUNIT_CALLBACKS);
        Method invokeBefores = callbacksClass.getDeclaredMethod(methodName);
        invokeBefores.invoke(null);
    }

}
