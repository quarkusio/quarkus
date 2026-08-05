package io.quarkus.arquillian;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Note that we cannot use event.getExecutor().invoke() directly because the callbacks would be invoked upon the original test
 * class instance and not the real test instance.
 * <p>
 * This class works for JUnit callbacks only, see {@link QuarkusTestNgCallbacks} for TestNG
 */
abstract class QuarkusJunitCallbacks {

    static void invokeJunitBefores(String className, Object testInstance)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException {
        // if there is no managed deployment, then we have no test instance because it hasn't been deployed yet
        if (testInstance != null) {
            List<Method> befores = new ArrayList<>();
            collectCallbacks(testInstance.getClass(), befores, (Class<? extends Annotation>) testInstance.getClass()
                    .getClassLoader().loadClass(className));
            for (Method before : befores) {
                invokeCallback(before, testInstance);
            }
        }
    }

    static void invokeJunitAfters(String className, Object testInstance)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException {
        if (testInstance != null) {
            List<Method> afters = new ArrayList<>();
            collectCallbacks(testInstance.getClass(), afters, (Class<? extends Annotation>) testInstance.getClass()
                    .getClassLoader().loadClass(className));
            for (Method after : afters) {
                invokeCallback(after, testInstance);
            }
        }
    }

    /**
     * A class level callback is static unless the test declares the per class test instance lifecycle, and
     * {@link Method#canAccess(Object)} rejects a non-null object for a static method.
     */
    private static void invokeCallback(Method callback, Object testInstance)
            throws IllegalAccessException, InvocationTargetException {
        Object target = Modifier.isStatic(callback.getModifiers()) ? null : testInstance;
        if (callback.canAccess(target) && callback.getParameters().length == 0) {
            callback.invoke(target);
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
