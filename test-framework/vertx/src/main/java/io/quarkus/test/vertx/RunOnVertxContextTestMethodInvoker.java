package io.quarkus.test.vertx;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.test.TestMethodInvoker;
import io.quarkus.test.TestReactiveTransaction;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

public class RunOnVertxContextTestMethodInvoker implements TestMethodInvoker {

    private UniAsserter uniAsserter;

    @Override
    public boolean handlesMethodParamType(String paramClassName) {
        return UniAsserter.class.getName().equals(paramClassName);
    }

    @Override
    public Object methodParamInstance(String paramClassName) {
        if (!handlesMethodParamType(paramClassName)) {
            throw new IllegalStateException(
                    this.getClass().getName() + " does not handle '" + paramClassName + "' method param types");
        }
        uniAsserter = createUniAsserter();
        return uniAsserter;
    }

    protected UniAsserter createUniAsserter() {
        return new DefaultUniAsserter();
    }

    @Override
    public boolean supportsMethod(Class<?> originalTestClass, Method originalTestMethod) {
        return hasSupportedAnnotation(originalTestClass, originalTestMethod) && hasSupportedParams(originalTestMethod);
    }

    private boolean hasSupportedParams(Method originalTestMethod) {
        return originalTestMethod.getParameterCount() == 0 || (originalTestMethod.getParameterCount() == 1
                // we need to use the class name to avoid ClassLoader issues
                && originalTestMethod.getParameterTypes()[0].getName().equals(UniAsserter.class.getName()));
    }

    protected boolean hasSupportedAnnotation(Class<?> originalTestClass, Method originalTestMethod) {
        return hasAnnotation(RunOnVertxContext.class, originalTestMethod.getAnnotations())
                || hasAnnotation(RunOnVertxContext.class, originalTestClass.getAnnotations())
                || hasAnnotation(TestReactiveTransaction.class, originalTestMethod.getAnnotations())
                || hasAnnotation(TestReactiveTransaction.class, originalTestClass.getAnnotations());
    }

    // we need to use the class name to avoid ClassLoader issues
    protected boolean hasAnnotation(Class<? extends Annotation> annotation, Annotation[] annotations) {
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
            throw new IllegalStateException(
                    "Vert.x instance has not been created before attempting to run test method '"
                            + actualTestMethod.getName() + "' of test class '" + testClassName + "'");
        }

        Context context = vertx.getOrCreateContext();
        Class<?> testClass = actualTestInstance != null ? actualTestInstance.getClass() : Object.class;
        boolean shouldDuplicateContext = shouldContextBeDuplicated(testClass, actualTestMethod);
        if (shouldDuplicateContext) {
            context = VertxContext.getOrCreateDuplicatedContext(context);
            VertxContextSafetyToggle.setContextSafe(context, true);
        }

        CompletableFuture<Object> cf;
        if (shouldRunOnEventLoop(testClass, actualTestMethod)) {
            cf = new CompletableFuture<>();
            var handler = new RunTestMethodOnVertxEventLoopContextHandler(actualTestInstance, actualTestMethod,
                    actualTestMethodArgs, uniAsserter, cf);
            context.runOnContext(handler);
        } else {
            var handler = new RunTestMethodOnVertxBlockingContextHandler(actualTestInstance, actualTestMethod,
                    actualTestMethodArgs, uniAsserter);
            cf = ((CompletableFuture<Object>) context.executeBlocking(handler).toCompletionStage());
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

    private boolean shouldContextBeDuplicated(Class<?> c, Method m) {
        RunOnVertxContext runOnVertxContext = m.getAnnotation(RunOnVertxContext.class);
        if (runOnVertxContext == null) {
            runOnVertxContext = c.getAnnotation(RunOnVertxContext.class);
        }
        if (runOnVertxContext == null) {
            // Use duplicated context if @TestReactiveTransaction is present
            return m.isAnnotationPresent(TestReactiveTransaction.class)
                    || m.getDeclaringClass().isAnnotationPresent(TestReactiveTransaction.class);
        } else {
            return runOnVertxContext.duplicateContext();
        }
    }

    private boolean shouldRunOnEventLoop(Class<?> c, Method m) {
        RunOnVertxContext runOnVertxContext = m.getAnnotation(RunOnVertxContext.class);
        if (runOnVertxContext == null) {
            runOnVertxContext = c.getAnnotation(RunOnVertxContext.class);
        }
        if (runOnVertxContext == null) {
            return true;
        } else {
            return runOnVertxContext.runOnEventLoop();
        }
    }

    public static class RunTestMethodOnVertxEventLoopContextHandler implements Handler<Void> {
        private static final Runnable DO_NOTHING = new Runnable() {
            @Override
            public void run() {
            }
        };

        private final Object testInstance;
        private final Method targetMethod;
        private final List<Object> methodArgs;
        private final UnwrappableUniAsserter uniAsserter;
        private final CompletableFuture<Object> future;

        public RunTestMethodOnVertxEventLoopContextHandler(Object testInstance, Method targetMethod,
                List<Object> methodArgs, UniAsserter uniAsserter, CompletableFuture<Object> future) {
            this.testInstance = testInstance;
            this.future = future;
            this.targetMethod = targetMethod;
            this.methodArgs = methodArgs;
            this.uniAsserter = (UnwrappableUniAsserter) uniAsserter;
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
                    uniAsserter.asUni().subscribe().with(new Consumer<Object>() {
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

    public static class RunTestMethodOnVertxBlockingContextHandler implements Handler<Promise<Object>> {
        private static final Runnable DO_NOTHING = () -> {
        };

        private final Object testInstance;
        private final Method targetMethod;
        private final List<Object> methodArgs;
        private final UnwrappableUniAsserter uniAsserter;

        public RunTestMethodOnVertxBlockingContextHandler(Object testInstance, Method targetMethod,
                List<Object> methodArgs, UniAsserter uniAsserter) {
            this.testInstance = testInstance;
            this.targetMethod = targetMethod;
            this.methodArgs = methodArgs;
            this.uniAsserter = (UnwrappableUniAsserter) uniAsserter;
        }

        @Override
        public void handle(Promise<Object> promise) {
            ManagedContext requestContext = Arc.container().requestContext();
            if (requestContext.isActive()) {
                doRun(promise, DO_NOTHING);
            } else {
                requestContext.activate();
                doRun(promise, new Runnable() {
                    @Override
                    public void run() {
                        requestContext.terminate();
                    }
                });
            }
        }

        private void doRun(Promise<Object> promise, Runnable onTerminate) {
            try {
                Object testMethodResult = targetMethod.invoke(testInstance, methodArgs.toArray(new Object[0]));
                if (uniAsserter != null) {
                    uniAsserter.asUni().subscribe().with(new Consumer<Object>() {
                        @Override
                        public void accept(Object o) {
                            onTerminate.run();
                            promise.complete();
                        }
                    }, new Consumer<>() {
                        @Override
                        public void accept(Throwable t) {
                            onTerminate.run();
                            promise.fail(t);
                        }
                    });
                } else {
                    onTerminate.run();
                    promise.complete(testMethodResult);
                }
            } catch (Throwable t) {
                onTerminate.run();
                promise.fail(t.getCause());
            }
        }
    }

}
