package io.quarkus.test.junit.vertx;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.quarkus.test.junit.TestMethodInvoker;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.vertx.core.Handler;

public class RunOnVertxContextTestMethodInvoker implements TestMethodInvoker {

    private ClassLoader actualTestCL;
    private Object uniResult;

    @Override
    public void init(ClassLoader actualTestCL) {
        this.actualTestCL = actualTestCL;
    }

    @Override
    public CustomMethodTypesHandler customMethodTypesHandler() {
        return new UniResultCustomMethodTypesHandler(this::setUniResult);
    }

    @Override
    public boolean supportsMethod(Class<?> originalTestClass, Method originalTestMethod) {
        return originalTestMethod.getAnnotation(RunOnVertxContext.class) != null
                || originalTestClass.isAnnotationPresent(RunOnVertxContext.class);
    }

    private void setUniResult(Object uniResult) {
        this.uniResult = uniResult;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object actualTestInstance, Method actualTestMethod, List<Object> actualTestMethodArgs,
            String testClassName) throws Throwable {
        Object vertxContext;
        try {
            // essentially does: VertxCoreRecorder.getVertx().get().getOrCreateContext()
            Class<?> vertxCoreRecorder = Class.forName(VertxCoreRecorder.class.getName(), false, actualTestCL);
            Supplier<Object> vertxSupplier = (Supplier<Object>) vertxCoreRecorder.getMethod("getVertx")
                    .invoke(null);
            Object vertx = vertxSupplier.get();
            if (vertx == null) {
                throw new IllegalStateException("Vert.x instance has not been created before attempting to run test method '"
                        + actualTestMethod.getName() + "' of test class '" + testClassName + "'");
            }
            vertxContext = vertx.getClass()
                    .getMethod("getOrCreateContext")
                    .invoke(vertx);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Unable to determine running Vert.x instance to run test method '"
                    + actualTestMethod.getName() + "' of test class '" + testClassName + "'", e);
        }

        Class<?> handlerClass;
        Object handler;
        CompletableFuture<Object> cf = new CompletableFuture<>();
        try {
            // essentially does: new RunTestMethodOnContextHandler(actualTestInstance, testMethod, testMethodArgs, uniAsserter, cf)
            handlerClass = Class.forName(Handler.class.getName(), false, actualTestCL);
            handler = Class.forName(RunTestMethodOnContextHandler.class.getName(), true, actualTestCL)
                    .getDeclaredConstructor(Object.class, Method.class, List.class, Object.class, CompletableFuture.class)
                    .newInstance(actualTestInstance, actualTestMethod, actualTestMethodArgs, uniResult, cf);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException
                | InstantiationException e) {
            throw new IllegalStateException("Unable to create a Vert.x Handler needed for running test method '"
                    + actualTestMethod.getName() + "' of test class '" + testClassName + "'", e);
        }

        try {
            // essentially does: context.runOnContext(handler)
            Class.forName(io.vertx.core.Context.class.getName(), false, actualTestCL)
                    .getMethod("runOnContext", handlerClass)
                    .invoke(vertxContext, handler);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Unable to invoke Vert.x Handler needed for running test method '"
                    + actualTestMethod.getName() + "' of test class '" + testClassName + "'", e);
        }

        try {
            return cf.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            // the test itself threw an exception
            throw e.getCause();
        }
    }

    private static class UniResultCustomMethodTypesHandler implements CustomMethodTypesHandler {

        private final Consumer<Object> uniResultConsumer;

        private UniResultCustomMethodTypesHandler(Consumer<Object> uniResultConsumer) {
            this.uniResultConsumer = uniResultConsumer;
        }

        @Override
        public List<Class<?>> handledTypes() {
            return Collections.singletonList(UniResult.class);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T instance(Class<?> clazz, ClassLoader actualTestClassLoader) {
            String name = clazz.getName();
            if (!name.equals(UniResult.class.getName())) {
                throw new IllegalStateException(
                        "This should never be called to provide an instance for anything other than class '" + name + "'");
            }
            try {
                Object uniResult = Class.forName(name, true, actualTestClassLoader).getConstructor().newInstance();
                uniResultConsumer.accept(uniResult);
                return (T) uniResult;
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException
                    | ClassNotFoundException e) {
                throw new IllegalStateException("Unable to create instance of '" + name + "' on test ClassLoader");
            }
        }
    }

    public static class RunTestMethodOnContextHandler implements Handler<Void> {
        private final Object testInstance;
        private final Method targetMethod;
        private final List<Object> methodArgs;
        private final Consumer<Object> asserter;
        private final CompletableFuture<Object> future;

        public RunTestMethodOnContextHandler(Object testInstance, Method targetMethod, List<Object> methodArgs,
                Object uniResult, CompletableFuture<Object> future) {
            this.testInstance = testInstance;
            this.future = future;
            this.targetMethod = targetMethod;
            this.methodArgs = methodArgs;
            this.asserter = uniResult != null ? new UniResultAsserter(targetMethod, testInstance.getClass(), uniResult, future)
                    : null;
        }

        @Override
        public void handle(Void event) {
            doRun();
        }

        private void doRun() {
            try {
                Object testMethodResult = targetMethod.invoke(testInstance, methodArgs.toArray(new Object[0]));
                if (asserter != null) {
                    // we expect the asserter to complete the future
                    asserter.accept(testMethodResult);
                } else {
                    future.complete(testMethodResult);
                }
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        }
    }

    /**
     * Returns a Runnable that asserts that the Uni created from the test matches the users expectations (by utilizing
     * the UniAsserter)
     *
     * This is horribly ugly because the UniAsserter is loaded from the TCCL so we need to use reflection
     * to perform all assertions.
     * Furthermore we need to make the result a runnable because it needs to run on the vert.x context write after the
     * test method has been invoked
     */
    static class UniResultAsserter implements Consumer<Object> {
        private final Method testMethod;
        private final Class<?> testClass;
        private final Object uniResult;
        private final CompletableFuture<Object> future;

        public UniResultAsserter(Method testMethod, Class<?> testClass, Object uniResult, CompletableFuture<Object> future) {
            this.testMethod = testMethod;
            this.testClass = testClass;
            this.uniResult = uniResult;
            this.future = future;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void accept(Object testMethodResult) {
            try {
                /*
                 * This whole piece of code essentially does:
                 *
                 * uniAsserter.getUni().subscribe().with(uniAsserter.getItemAssertion(), (t) -> throw new
                 * RuntimeException("bla bla", t)).await().indefinitely();
                 */

                Class<?> uniAsserterClass = Class.forName(UniResult.class.getName(), false,
                        Thread.currentThread().getContextClassLoader());
                Object uni = uniAsserterClass
                        .getMethod("getUni")
                        .invoke(uniResult);
                Consumer<Object> itemAssertion = (Consumer<Object>) uniAsserterClass
                        .getMethod("getItemAssertion")
                        .invoke(uniResult);
                Object uniSubscribe = uni.getClass().getMethod("subscribe").invoke(uni);
                uniSubscribe.getClass().getMethod("with", Consumer.class, Consumer.class).invoke(uniSubscribe,
                        new Consumer<Object>() {
                            @Override
                            public void accept(Object o) {
                                try {
                                    itemAssertion.accept(o);
                                    future.complete(testMethodResult);
                                } catch (Throwable t) {
                                    future.completeExceptionally(t);
                                }
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable t) {
                                future.completeExceptionally(t);
                            }
                        });
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Unable to subscribe to the result of test method '"
                        + testMethod.getName() + "' of test class '" + testClass.getName() + "'", e);
            }
        }
    }
}
