package io.quarkus.arquillian;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jboss.arquillian.container.spi.client.deployment.Deployment;
import org.jboss.arquillian.container.spi.client.deployment.DeploymentScenario;
import org.jboss.arquillian.container.spi.context.annotation.DeploymentScoped;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.impl.RunModeUtils;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.test.spi.annotation.ClassScoped;
import org.jboss.arquillian.test.spi.event.suite.TestLifecycleEvent;

public class QuarkusBeforeAfterLifecycle {

    private static final String JUNIT4_CALLBACKS = "io.quarkus.arquillian.QuarkusJunit4Callbacks";
    private static final String JUNIT5_CALLBACKS = "io.quarkus.arquillian.QuarkusJunit5Callbacks";
    private static final String TESTNG_CALLBACKS = "io.quarkus.arquillian.QuarkusTestNgCallbacks";
    private static final String JUNIT_INVOKE_BEFORES = "invokeJunitBefores";
    private static final String JUNIT_INVOKE_AFTERS = "invokeJunitAfters";
    private static final String JUNIT_INVOKE_BEFORE_ALLS = "invokeJunitBeforeAlls";
    private static final String JUNIT_INVOKE_AFTER_ALLS = "invokeJunitAfterAlls";
    private static final String TESTNG_INVOKE_BEFORE_CLASS = "invokeTestNgBeforeClasses";
    private static final String TESTNG_INVOKE_AFTER_CLASS = "invokeTestNgAfterClasses";
    private static final String TESTNG_INVOKE_BEFORE_METHOD = "invokeTestNgBeforeMethods";
    private static final String TESTNG_INVOKE_AFTER_METHOD = "invokeTestNgAfterMethods";

    private static final int DEFAULT_PRECEDENCE = -100;

    /**
     * The managed deployment is undeployed by an {@code AfterClass} observer of default precedence, and undeploying
     * discards the in container test instance, so the class level callbacks have to be invoked before that happens.
     */
    private static final int AFTER_CLASS_PRECEDENCE = 100;

    @Inject
    @DeploymentScoped
    private Instance<QuarkusDeployment> quarkusDeployment;

    @Inject
    private Instance<Deployment> deployment;

    @Inject
    @ClassScoped
    private Instance<DeploymentScenario> deploymentScenario;

    public void on(@Observes(precedence = DEFAULT_PRECEDENCE) org.jboss.arquillian.test.spi.event.suite.Before event)
            throws Throwable {
        if (!isRunAsClient(event)) {
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
        if (!isRunAsClient(event)) {
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
        if (!isRunAsClient(event.getTestClass())) {
            if (isJunit5Available()) {
                invokeCallbacks(JUNIT_INVOKE_BEFORE_ALLS, JUNIT5_CALLBACKS);
            }
            if (isTestNGAvailable()) {
                invokeCallbacks(TESTNG_INVOKE_BEFORE_CLASS, TESTNG_CALLBACKS);
            }
        }
    }

    public void afterClass(
            @Observes(precedence = AFTER_CLASS_PRECEDENCE) org.jboss.arquillian.test.spi.event.suite.AfterClass event)
            throws Throwable {
        if (!isRunAsClient(event.getTestClass())) {
            if (isJunit5Available()) {
                invokeCallbacks(JUNIT_INVOKE_AFTER_ALLS, JUNIT5_CALLBACKS);
            }
            if (isTestNGAvailable()) {
                invokeCallbacks(TESTNG_INVOKE_AFTER_CLASS, TESTNG_CALLBACKS);
            }
        }
    }

    /**
     * A test which runs as client is executed on the test instance created by the test framework itself, so its
     * callbacks are already invoked by the test framework and must not be invoked on the in container test instance
     * again. Next to {@link RunAsClient}, this is also the case for a deployment declared as not testable.
     */
    private boolean isRunAsClient(TestLifecycleEvent event) {
        return RunModeUtils.isRunAsClient(deployment.get(), event.getTestClass(), event.getTestMethod());
    }

    /**
     * The deployment context is not active during the class level events, so the run mode can there only be determined
     * from the deployment scenario of the class.
     */
    private boolean isRunAsClient(TestClass testClass) {
        if (testClass.isAnnotationPresent(RunAsClient.class)) {
            return true;
        }
        DeploymentScenario scenario = deploymentScenario.get();
        return scenario == null || scenario.deployments().stream().noneMatch(d -> d.getDescription().testable());
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
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            NoSuchMethodException, SecurityException, ClassNotFoundException {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        ClassLoader cl = quarkusDeployment.get() != null && quarkusDeployment.get().hasAppClassLoader()
                ? quarkusDeployment.get().getAppClassLoader()
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
