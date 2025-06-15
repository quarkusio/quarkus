package io.quarkus.arquillian;

import java.lang.reflect.InvocationTargetException;

/**
 * Note that we cannot use event.getExecutor().invoke() directly because the callbacks would be invoked upon the
 * original test class instance and not the real test instance.
 * <p>
 * This class works for JUnit callbacks only, see {@link QuarkusTestNgCallbacks} for TestNG
 */
class QuarkusJunit4Callbacks extends QuarkusJunitCallbacks {

    private static final String BEFORE_EACH_CLASS_NAME_JUNIT4 = "org.junit.Before";
    private static final String AFTER_EACH_CLASS_NAME_JUNIT4 = "org.junit.After";

    static void invokeJunitBefores(Object testInstance)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException {
        invokeJunitBefores(BEFORE_EACH_CLASS_NAME_JUNIT4, testInstance);
    }

    static void invokeJunitAfters(Object testInstance)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException {
        invokeJunitAfters(AFTER_EACH_CLASS_NAME_JUNIT4, testInstance);
    }
}
