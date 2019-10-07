package io.quarkus.arquillian;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * Note that we cannot use event.getExecutor().invoke() directly because the callbacks would be invoked upon the original test
 * class instance and not the real test instance.
 * <p>
 * This class works for TestNG only, see {@link QuarkusJunitCallbacks} for Junit bits
 */
public class QuarkusTestNgCallbacks {

    private static final String ARQ_TESTNG_SUPERCLASS = "org.jboss.arquillian.testng.Arquillian";

    static void invokeTestNgBeforeClasses() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Object testInstance = QuarkusDeployableContainer.testInstance;
        if (testInstance != null) {
            List<Method> beforeClasses = new ArrayList<>();
            collectCallbacks(testInstance.getClass(), beforeClasses, BeforeClass.class);
            for (Method beforeClass : beforeClasses) {
                beforeClass.invoke(testInstance);
            }
        }
    }

    static void invokeTestNgAfterClasses() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Object testInstance = QuarkusDeployableContainer.testInstance;
        if (testInstance != null) {
            List<Method> afterClasses = new ArrayList<>();
            collectCallbacks(testInstance.getClass(), afterClasses, AfterClass.class);
            for (Method afterClass : afterClasses) {
                afterClass.invoke(testInstance);
            }
        }
    }

    private static void collectCallbacks(Class<?> testClass, List<Method> callbacks, Class<? extends Annotation> annotation) {
        Arrays.stream(testClass.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(annotation)).forEach(callbacks::add);
        Class<?> superClass = testClass.getSuperclass();
        if (superClass != null && !superClass.equals(Object.class) &&
        // TestNG Arq. superclass uses lifecycle methods as well, we don't want to pick up those
                !superClass.toString().contains(ARQ_TESTNG_SUPERCLASS)) {
            collectCallbacks(superClass, callbacks, annotation);
        }
    }
}
