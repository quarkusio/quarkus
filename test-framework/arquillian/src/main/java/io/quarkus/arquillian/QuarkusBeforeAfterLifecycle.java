package io.quarkus.arquillian;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jboss.arquillian.container.spi.context.annotation.DeploymentScoped;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class QuarkusBeforeAfterLifecycle {

    private static final String JUNIT_CALLBACKS = "io.quarkus.arquillian.QuarkusJunitCallbacks";
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
    private InstanceProducer<ClassLoader> appClassloader;

    public void on(@Observes(precedence = DEFAULT_PRECEDENCE) org.jboss.arquillian.test.spi.event.suite.Before event)
            throws Throwable {
        if (isJunitAvailable()) {
            invokeCallbacks(JUNIT_INVOKE_BEFORES, JUNIT_CALLBACKS);
        }
        if (isTestNGAvailable()) {
            invokeCallbacks(TESTNG_INVOKE_BEFORE_METHOD, TESTNG_CALLBACKS);
        }
    }

    public void on(@Observes(precedence = DEFAULT_PRECEDENCE) org.jboss.arquillian.test.spi.event.suite.After event)
            throws Throwable {
        if (isJunitAvailable()) {
            invokeCallbacks(JUNIT_INVOKE_AFTERS, JUNIT_CALLBACKS);
        }
        if (isTestNGAvailable()) {
            invokeCallbacks(TESTNG_INVOKE_AFTER_METHOD, TESTNG_CALLBACKS);
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

    private boolean isJunitAvailable() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            cl.loadClass("org.jboss.arquillian.junit.container.JUnitTestRunner");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private boolean isTestNGAvailable() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            cl.loadClass("org.jboss.arquillian.testng.container.TestNGTestRunner");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void invokeCallbacks(String methodName, String junitOrTestNgCallbackClass)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            NoSuchMethodException, SecurityException, ClassNotFoundException {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        ClassLoader cl = appClassloader.get() != null ? appClassloader.get() : old;

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
