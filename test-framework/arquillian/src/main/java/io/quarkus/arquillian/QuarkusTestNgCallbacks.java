package io.quarkus.arquillian;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;

/**
 * Note that we cannot use event.getExecutor().invoke() directly because the callbacks would be invoked upon the original test
 * class instance and not the real test instance.
 * <p>
 * This class works for TestNG only, see {@link QuarkusJunitCallbacks} for Junit bits
 */
public class QuarkusTestNgCallbacks {

    private static final String ARQ_TESTNG_SUPERCLASS = "org.jboss.arquillian.testng.Arquillian";

    static void invokeTestNgBeforeClasses(Object testInstance)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException {
        if (testInstance != null) {
            List<Method> beforeClasses = new ArrayList<>();
            collectCallbacks(testInstance.getClass(), beforeClasses, (Class<? extends Annotation>) testInstance.getClass()
                    .getClassLoader().loadClass(BeforeClass.class.getName()));
            for (Method m : beforeClasses) {
                // we don't know the values for parameterized methods that TestNG allows, we just skip those
                if (m.getParameterCount() == 0) {
                    m.setAccessible(true);
                    m.invoke(testInstance);
                }
            }
        }
    }

    static void invokeTestNgAfterClasses(Object testInstance)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException {
        if (testInstance != null) {
            List<Method> afterClasses = new ArrayList<>();
            collectCallbacks(testInstance.getClass(), afterClasses, (Class<? extends Annotation>) testInstance.getClass()
                    .getClassLoader().loadClass(AfterClass.class.getName()));
            for (Method m : afterClasses) {
                // we don't know the values for parameterized methods that TestNG allows, we just skip those
                if (m.getParameterCount() == 0) {
                    m.setAccessible(true);
                    m.invoke(testInstance);
                }
            }
        }
    }

    static void invokeTestNgAfterMethods(Object testInstance)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException {
        if (testInstance != null) {
            List<Method> afterMethods = new ArrayList<>();
            collectCallbacks(testInstance.getClass(), afterMethods, (Class<? extends Annotation>) testInstance.getClass()
                    .getClassLoader().loadClass(AfterMethod.class.getName()));
            collectCallbacks(testInstance.getClass(), afterMethods, (Class<? extends Annotation>) testInstance.getClass()
                    .getClassLoader().loadClass(AfterTest.class.getName()));
            for (Method m : afterMethods) {
                // we don't know the values for parameterized methods that TestNG allows, we just skip those
                if (m.getParameterCount() == 0) {
                    m.setAccessible(true);
                    m.invoke(testInstance);
                }
            }
        }
    }

    static void invokeTestNgBeforeMethods(Object testInstance)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException {
        if (testInstance != null) {
            List<Method> beforeMethods = new ArrayList<>();
            collectCallbacks(testInstance.getClass(), beforeMethods, (Class<? extends Annotation>) testInstance.getClass()
                    .getClassLoader().loadClass(BeforeMethod.class.getName()));
            collectCallbacks(testInstance.getClass(), beforeMethods, (Class<? extends Annotation>) testInstance.getClass()
                    .getClassLoader().loadClass(BeforeTest.class.getName()));
            for (Method m : beforeMethods) {
                // we don't know the values for parameterized methods that TestNG allows, we just skip those
                if (m.getParameterCount() == 0) {
                    m.setAccessible(true);
                    m.invoke(testInstance);
                }
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
