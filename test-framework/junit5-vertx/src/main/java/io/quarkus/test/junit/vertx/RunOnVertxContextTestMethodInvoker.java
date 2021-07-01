package io.quarkus.test.junit.vertx;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.test.TestReactiveTransaction;
import io.quarkus.test.junit.TestMethodInvoker;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class RunOnVertxContextTestMethodInvoker implements TestMethodInvoker {

    private DefaultUniAsserter uniAsserter;

    @Override
    public boolean handlesMethodParamType(String paramClassName) {
        return UniAsserter.class.getName().equals(paramClassName);
    }

    @Override
    public Object methodParamInstance(String paramClassName) {
        if (!handlesMethodParamType(paramClassName)) {
            throw new IllegalStateException(
                    "RunOnVertxContextTestMethodInvoker does not handle '" + paramClassName + "' method param types");
        }
        uniAsserter = new DefaultUniAsserter();
        return uniAsserter;
    }

    @Override
    public boolean supportsMethod(Class<?> originalTestClass, Method originalTestMethod) {
        return hasAnnotation(RunOnVertxContext.class, originalTestMethod.getAnnotations())
                || hasAnnotation(RunOnVertxContext.class, originalTestClass.getAnnotations())
                || hasAnnotation("io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional",
                        originalTestMethod.getAnnotations())
                || hasAnnotation("io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional",
                        originalTestClass.getAnnotations())
                || hasAnnotation(TestReactiveTransaction.class, originalTestMethod.getAnnotations())
                || hasAnnotation(TestReactiveTransaction.class, originalTestClass.getAnnotations());
    }

    // we need to use the class name to avoid ClassLoader issues
    private boolean hasAnnotation(Class<? extends Annotation> annotation, Annotation[] annotations) {
        return hasAnnotation(annotation.getName(), annotations);
    }

    private boolean hasAnnotation(String annotationName, Annotation[] annotations) {
        if (annotations != null) {
            for (Annotation methodAnnotation : annotations) {
                if (annotationName.equals(methodAnnotation.annotationType().getName())) {
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
                actualTestMethodArgs, uniAsserter, cf);
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
        private static final Runnable DO_NOTHING = new Runnable() {
            @Override
            public void run() {
            }
        };

        private final Object testInstance;
        private final Method targetMethod;
        private final List<Object> methodArgs;
        private final DefaultUniAsserter uniAsserter;
        private final CompletableFuture<Object> future;

        public RunTestMethodOnContextHandler(Object testInstance, Method targetMethod, List<Object> methodArgs,
                DefaultUniAsserter uniAsserter, CompletableFuture<Object> future) {
            this.testInstance = testInstance;
            this.future = future;
            this.targetMethod = targetMethod;
            this.methodArgs = methodArgs;
            this.uniAsserter = uniAsserter;
        }

        @Override
        public void handle(Void event) {
            ManagedContext requestContext = Arc.container().requestContext();
            if (requestContext.isActive()) {
                doRun(DO_NOTHING);
            } else {
                requestContext.activate();
                doRun(new Runnable() {
                    @Override
                    public void run() {
                        requestContext.terminate();
                    }
                });
            }
        }

        private void doRun(Runnable onTerminate) {
            try {
                Object testMethodResult = targetMethod.invoke(testInstance, methodArgs.toArray(new Object[0]));
                if (uniAsserter != null) {
                    uniAsserter.execution.subscribe().with(new Consumer<Object>() {
                        @Override
                        public void accept(Object o) {
                            onTerminate.run();
                            future.complete(testMethodResult);
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable t) {
                            onTerminate.run();
                            future.completeExceptionally(t);
                        }
                    });
                } else {
                    onTerminate.run();
                    future.complete(testMethodResult);
                }
            } catch (Throwable t) {
                onTerminate.run();
                future.completeExceptionally(t.getCause());
            }
        }
    }

}
