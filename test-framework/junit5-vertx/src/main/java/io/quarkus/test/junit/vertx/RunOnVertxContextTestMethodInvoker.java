package io.quarkus.test.junit.vertx;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import io.quarkus.test.junit.TestMethodInvoker;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class RunOnVertxContextTestMethodInvoker implements TestMethodInvoker {

    private UniResult uniResult;

    @Override
    public boolean handlesMethodParamType(String paramClassName) {
        return UniResult.class.getName().equals(paramClassName);
    }

    @Override
    public Object methodParamInstance(String paramClassName) {
        if (!handlesMethodParamType(paramClassName)) {
            throw new IllegalStateException(
                    "RunOnVertxContextTestMethodInvoker does not handle '" + paramClassName + "' method param types");
        }
        uniResult = new UniResult<>();
        return uniResult;
    }

    @Override
    public boolean supportsMethod(Class<?> originalTestClass, Method originalTestMethod) {
        return hasAnnotation(RunOnVertxContext.class, originalTestMethod.getAnnotations())
                || hasAnnotation(RunOnVertxContext.class, originalTestClass.getAnnotations());
    }

    // we need to use the class name to avoid ClassLoader issues
    private boolean hasAnnotation(Class<? extends Annotation> annotation, Annotation[] annotations) {
        if (annotations != null) {
            for (Annotation methodAnnotation : annotations) {
                if (annotation.getName().equals(methodAnnotation.annotationType().getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Object invoke(Object actualTestInstance, Method actualTestMethod, List<Object> actualTestMethodArgs,
            String testClassName) throws Throwable {

        Vertx vertx = VertxCoreRecorder.getVertx().get();
        if (vertx == null) {
            throw new IllegalStateException("Vert.x instance has not been created before attempting to run test method '"
                    + actualTestMethod.getName() + "' of test class '" + testClassName + "'");
        }
        CompletableFuture<Object> cf = new CompletableFuture<>();
        RunTestMethodOnContextHandler handler = new RunTestMethodOnContextHandler(actualTestInstance, actualTestMethod,
                actualTestMethodArgs, uniResult, cf);
        vertx.getOrCreateContext().runOnContext(handler);
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

    public static class RunTestMethodOnContextHandler implements Handler<Void> {
        private final Object testInstance;
        private final Method targetMethod;
        private final List<Object> methodArgs;
        private final Consumer<Object> asserter;
        private final CompletableFuture<Object> future;

        public RunTestMethodOnContextHandler(Object testInstance, Method targetMethod, List<Object> methodArgs,
                UniResult uniResult, CompletableFuture<Object> future) {
            this.testInstance = testInstance;
            this.future = future;
            this.targetMethod = targetMethod;
            this.methodArgs = methodArgs;
            this.asserter = uniResult != null ? new UniResultAsserter(uniResult, future)
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
     * Returns a Consumer that asserts that the Uni created from the test matches the users expectations (by utilizing
     * the UniAsserter)
     */
    static class UniResultAsserter implements Consumer<Object> {
        private final UniResult uniResult;
        private final CompletableFuture<Object> future;

        public UniResultAsserter(UniResult uniResult, CompletableFuture<Object> future) {
            this.uniResult = uniResult;
            this.future = future;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void accept(Object testMethodResult) {
            uniResult.getUni().subscribe().with(
                    new Consumer<Object>() {
                        @Override
                        public void accept(Object o) {
                            try {
                                uniResult.getItemAssertion().accept(o);
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
        }
    }
}
