package io.quarkus.arquillian;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jboss.arquillian.container.spi.context.annotation.DeploymentScoped;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class QuarkusBeforeAfterLifecycle {

    private static final String JUNIT4_CALLBACKS = "io.quarkus.arquillian.QuarkusJunit4Callbacks";
    private static final String JUNIT5_CALLBACKS = "io.quarkus.arquillian.QuarkusJunit5Callbacks";
    private static final String TESTNG_CALLBACKS = "io.quarkus.arquillian.QuarkusTestNgCallbacks";
    private static final String JUNIT_INVOKE_BEFORES = "invokeJunitBefores";
    private static final String JUNIT_INVOKE_AFTERS = "invokeJunitAfters";
    private static final String TESTNG_INVOKE_BEFORE_CLASS = "invokeTestNgBeforeClasses";
    private static final String TESTNG_INVOKE_AFTER_CLASS = "invokeTestNgAfterClasses";
    private static final String TESTNG_INVOKE_BEFORE_METHOD = "invokeTestNgBeforeMethods";
    private static final String TESTNG_INVOKE_AFTER_METHOD = "invokeTestNgAfterMethods";

    private static final int DEFAULT_PRECEDENCE = -100;

    @Inject
    @DeploymentScoped
    private Instance<QuarkusDeployment> deployment;

    public void on(@Observes(precedence = DEFAULT_PRECEDENCE) org.jboss.arquillian.test.spi.event.suite.Before event)
            throws Throwable {
        if (!event.getTestClass().isAnnotationPresent(RunAsClient.class)) {
            if (isJunit5Available()) {
                invokeCallbacks(JUNIT_INVOKE_BEFORES, JUNIT5_CALLBACKS);
            } else if (isJunit4Available()) {
                invokeCallbacks(JUNIT_INVOKE_BEFORES, JUNIT4_CALLBACKS);
            }
            if (isTestNGAvailable()) {
                invokeCallbacks(TESTNG_INVOKE_BEFORE_METHOD, TESTNG_CALLBACKS);
            }
        }
    }

    public void on(@Observes(precedence = DEFAULT_PRECEDENCE) org.jboss.arquillian.test.spi.event.suite.After event)
            throws Throwable {
        if (!event.getTestClass().isAnnotationPresent(RunAsClient.class)) {
            if (isJunit5Available()) {
                invokeCallbacks(JUNIT_INVOKE_AFTERS, JUNIT5_CALLBACKS);
            } else if (isJunit4Available()) {
                invokeCallbacks(JUNIT_INVOKE_AFTERS, JUNIT4_CALLBACKS);
            }
            if (isTestNGAvailable()) {
                invokeCallbacks(TESTNG_INVOKE_AFTER_METHOD, TESTNG_CALLBACKS);
            }
        }
    }

    public void beforeClass(
            @Observes(precedence = DEFAULT_PRECEDENCE) org.jboss.arquillian.test.spi.event.suite.BeforeClass event)
            throws Throwable {
        if (isTestNGAvailable()) {
            invokeCallbacks(TESTNG_INVOKE_BEFORE_CLASS, TESTNG_CALLBACKS);
        }
    }

    public void afterClass(
            @Observes(precedence = DEFAULT_PRECEDENCE) org.jboss.arquillian.test.spi.event.suite.AfterClass event)
            throws Throwable {
        if (isTestNGAvailable()) {
            invokeCallbacks(TESTNG_INVOKE_AFTER_CLASS, TESTNG_CALLBACKS);
        }
    }

    private boolean isJunit5Available() {
        return isClassAvailable("org.jboss.arquillian.junit5.ArquillianExtension");
    }

    private boolean isJunit4Available() {
        return isClassAvailable("org.jboss.arquillian.junit.container.JUnitTestRunner");
    }

    private boolean isTestNGAvailable() {
        return isClassAvailable("org.jboss.arquillian.testng.container.TestNGTestRunner");
    }

    private boolean isClassAvailable(String className) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            cl.loadClass(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void invokeCallbacks(String methodName, String junitOrTestNgCallbackClass)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
            SecurityException, ClassNotFoundException {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        ClassLoader cl = deployment.get() != null && deployment.get().hasAppClassLoader()
                ? deployment.get().getAppClassLoader()
                : old;

        try {
            Thread.currentThread().setContextClassLoader(cl);
            Class<?> callbacksClass = cl.loadClass(junitOrTestNgCallbackClass);
            Method declaredMethod = callbacksClass.getDeclaredMethod(methodName, Object.class);
            declaredMethod.setAccessible(true);
            declaredMethod.invoke(null, QuarkusDeployableContainer.testInstance);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

}
