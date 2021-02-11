package io.quarkus.arquillian;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;

/**
 * Note that we cannot use event.getExecutor().invoke() directly because the callbacks would be invoked upon the original test
 * class instance and not the real test instance.
 * <p>
 * This class works for JUnit callbacks only, see {@link QuarkusTestNgCallbacks} for TestNG
 */
class QuarkusJunitCallbacks {

    static void invokeJunitBefores(Object testInstance)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException {
        // if there is no managed deployment, then we have no test instance because it hasn't been deployed yet
        if (testInstance != null) {
            List<Method> befores = new ArrayList<>();
            collectCallbacks(testInstance.getClass(), befores, (Class<? extends Annotation>) testInstance.getClass()
                    .getClassLoader().loadClass(Before.class.getName()));
            for (Method before : befores) {
                before.invoke(testInstance);
            }
        }
    }

    static void invokeJunitAfters(Object testInstance)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException {
        if (testInstance != null) {
            List<Method> afters = new ArrayList<>();
            collectCallbacks(testInstance.getClass(), afters, (Class<? extends Annotation>) testInstance.getClass()
                    .getClassLoader().loadClass(After.class.getName()));
            for (Method after : afters) {
                after.invoke(testInstance);
            }
        }
    }

    private static void collectCallbacks(Class<?> testClass, List<Method> callbacks, Class<? extends Annotation> annotation) {
        Arrays.stream(testClass.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(annotation)).forEach(callbacks::add);
        Class<?> superClass = testClass.getSuperclass();
        if (superClass != null && !superClass.equals(Object.class)) {
            collectCallbacks(superClass, callbacks, annotation);
        }
    }
}
