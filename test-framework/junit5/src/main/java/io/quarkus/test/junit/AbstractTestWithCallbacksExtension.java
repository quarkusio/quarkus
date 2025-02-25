package io.quarkus.test.junit;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import io.quarkus.test.junit.callback.QuarkusTestAfterAllCallback;
import io.quarkus.test.junit.callback.QuarkusTestAfterConstructCallback;
import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestAfterTestExecutionCallback;
import io.quarkus.test.junit.callback.QuarkusTestBeforeClassCallback;
import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestBeforeTestExecutionCallback;
import io.quarkus.test.junit.callback.QuarkusTestContext;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;

public abstract class AbstractTestWithCallbacksExtension {
    private static List<Object> beforeClassCallbacks;
    private static List<Object> afterConstructCallbacks;
    private static List<Object> beforeEachCallbacks;
    private static List<Object> beforeTestCallbacks;
    private static List<Object> afterTestCallbacks;
    private static List<Object> afterEachCallbacks;
    private static List<Object> afterAllCallbacks;

    protected boolean isBeforeTestCallbacksEmpty() {
        return beforeTestCallbacks == null || beforeTestCallbacks.isEmpty();
    }

    protected void invokeBeforeTestExecutionCallbacks(QuarkusTestMethodContext quarkusTestMethodContext) throws Exception {
        invokeBeforeTestExecutionCallbacks(QuarkusTestMethodContext.class, quarkusTestMethodContext);
    }

    protected void invokeBeforeTestExecutionCallbacks(Class<?> clazz, Object classInstance) throws Exception {
        invokeCallbacks(beforeTestCallbacks, "beforeTestExecution", clazz, classInstance);
    }

    protected boolean isAfterTestCallbacksEmpty() {
        return afterTestCallbacks == null || afterTestCallbacks.isEmpty();
    }

    protected void invokeAfterTestExecutionCallbacks(QuarkusTestMethodContext quarkusTestMethodContext) throws Exception {
        invokeAfterTestExecutionCallbacks(QuarkusTestMethodContext.class, quarkusTestMethodContext);
    }

    protected void invokeAfterTestExecutionCallbacks(Class<?> clazz, Object classInstance) throws Exception {
        invokeCallbacks(afterTestCallbacks, "afterTestExecution", clazz, classInstance);
    }

    protected void invokeBeforeClassCallbacks(Class<?> classInstance) throws Exception {
        invokeBeforeClassCallbacks(Class.class, classInstance);
    }

    protected void invokeBeforeClassCallbacks(Class<?> clazz, Object classInstance) throws Exception {
        invokeCallbacks(beforeClassCallbacks, "beforeClass", clazz, classInstance);
    }

    protected void invokeAfterConstructCallbacks(Object testInstance) throws Exception {
        invokeAfterConstructCallbacks(Object.class, testInstance);
    }

    protected void invokeAfterConstructCallbacks(Class<?> clazz, Object classInstance) throws Exception {
        invokeCallbacks(afterConstructCallbacks, "afterConstruct", clazz, classInstance);
    }

    protected boolean isBeforeEachCallbacksEmpty() {
        return beforeEachCallbacks == null || beforeEachCallbacks.isEmpty();
    }

    protected void invokeBeforeEachCallbacks(QuarkusTestMethodContext quarkusTestMethodContext) throws Exception {
        invokeBeforeEachCallbacks(QuarkusTestMethodContext.class, quarkusTestMethodContext);
    }

    protected void invokeBeforeEachCallbacks(Class<?> clazz, Object classInstance) throws Exception {
        invokeCallbacks(beforeEachCallbacks, "beforeEach", clazz, classInstance);
    }

    protected void invokeAfterEachCallbacks(QuarkusTestMethodContext testMethodContext) throws Exception {
        invokeAfterEachCallbacks(QuarkusTestMethodContext.class, testMethodContext);
    }

    protected boolean isAfterEachCallbacksEmpty() {
        return afterEachCallbacks == null || afterEachCallbacks.isEmpty();
    }

    protected void invokeAfterEachCallbacks(Class<?> clazz, Object classInstance) throws Exception {
        invokeCallbacks(afterEachCallbacks, "afterEach", clazz, classInstance);
    }

    protected boolean isAfterAllCallbacksEmpty() {
        return afterAllCallbacks == null || afterAllCallbacks.isEmpty();
    }

    protected void invokeAfterAllCallbacks(QuarkusTestContext testContext) throws Exception {
        invokeAfterAllCallbacks(QuarkusTestContext.class, testContext);
    }

    protected void invokeAfterAllCallbacks(Class<?> clazz, Object testContext) throws Exception {
        invokeCallbacks(afterAllCallbacks, "afterAll", clazz, testContext);
    }

    protected static void clearCallbacks() {
        beforeClassCallbacks = new ArrayList<>();
        afterConstructCallbacks = new ArrayList<>();
        beforeEachCallbacks = new ArrayList<>();
        beforeTestCallbacks = new ArrayList<>();
        afterTestCallbacks = new ArrayList<>();
        afterEachCallbacks = new ArrayList<>();
        afterAllCallbacks = new ArrayList<>();
    }

    protected void populateCallbacks(ClassLoader classLoader) throws ClassNotFoundException {
        clearCallbacks();

        ServiceLoader<?> quarkusTestBeforeClassLoader = ServiceLoader
                .load(Class.forName(QuarkusTestBeforeClassCallback.class.getName(), false, classLoader), classLoader);
        for (Object quarkusTestBeforeClassCallback : quarkusTestBeforeClassLoader) {
            beforeClassCallbacks.add(quarkusTestBeforeClassCallback);
        }
        ServiceLoader<?> quarkusTestAfterConstructLoader = ServiceLoader
                .load(Class.forName(QuarkusTestAfterConstructCallback.class.getName(), false, classLoader), classLoader);
        for (Object quarkusTestAfterConstructCallback : quarkusTestAfterConstructLoader) {
            afterConstructCallbacks.add(quarkusTestAfterConstructCallback);
        }
        ServiceLoader<?> quarkusTestBeforeEachLoader = ServiceLoader
                .load(Class.forName(QuarkusTestBeforeEachCallback.class.getName(), false, classLoader), classLoader);
        for (Object quarkusTestBeforeEachCallback : quarkusTestBeforeEachLoader) {
            beforeEachCallbacks.add(quarkusTestBeforeEachCallback);
        }
        ServiceLoader<?> quarkusTestBeforeTestLoader = ServiceLoader
                .load(Class.forName(QuarkusTestBeforeTestExecutionCallback.class.getName(), false, classLoader), classLoader);
        for (Object quarkusTestBeforeTestCallback : quarkusTestBeforeTestLoader) {
            beforeTestCallbacks.add(quarkusTestBeforeTestCallback);
        }
        ServiceLoader<?> quarkusTestAfterTestLoader = ServiceLoader
                .load(Class.forName(QuarkusTestAfterTestExecutionCallback.class.getName(), false, classLoader), classLoader);
        for (Object quarkusTestAfterTestCallback : quarkusTestAfterTestLoader) {
            afterTestCallbacks.add(quarkusTestAfterTestCallback);
        }
        ServiceLoader<?> quarkusTestAfterEachLoader = ServiceLoader
                .load(Class.forName(QuarkusTestAfterEachCallback.class.getName(), false, classLoader), classLoader);
        for (Object quarkusTestAfterEach : quarkusTestAfterEachLoader) {
            afterEachCallbacks.add(quarkusTestAfterEach);
        }
        ServiceLoader<?> quarkusTestAfterAllLoader = ServiceLoader
                .load(Class.forName(QuarkusTestAfterAllCallback.class.getName(), false, classLoader), classLoader);
        for (Object quarkusTestAfterAll : quarkusTestAfterAllLoader) {
            afterAllCallbacks.add(quarkusTestAfterAll);
        }
    }

    private void invokeCallbacks(List<Object> callbacks, String methodName, Class<?> clazz, Object classInstance)
            throws Exception {
        if (callbacks == null || callbacks.isEmpty()) {
            return;
        }

        try {
            for (Object callback : callbacks) {
                callback.getClass().getMethod(methodName, clazz)
                        .invoke(callback, classInstance);
            }
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            } else if (e.getCause() instanceof AssertionError) {
                throw (AssertionError) e.getCause();
            }
            throw e;
        }
    }

    protected Class getTestingType() {

        // In general, the testing type should be the test extension class, but ...
        Class type = this.getClass();

        // We don't want to pick up the class of anonymous classes, since they're clearly supposed to 'be' the superclass.
        // We want something like
        //        @RegisterExtension
        //        static QuarkusTestExtension TEST = new QuarkusTestExtension() {
        //        @Override
        //        // Whatever
        //  };
        // to count as a QuarkusTestExtension class
        if (type.isAnonymousClass()) {
            return type.getSuperclass();
        } else {
            return type;
        }
    }
}
