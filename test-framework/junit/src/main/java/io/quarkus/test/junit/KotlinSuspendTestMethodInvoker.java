package io.quarkus.test.junit;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import io.quarkus.test.TestMethodInvoker;

public class KotlinSuspendTestMethodInvoker implements TestMethodInvoker {

    private static final String KOTLIN_RESULT_FAILURE_CLASS_NAME = "kotlin.Result$Failure";

    @Override
    public boolean supportsMethod(Class<?> originalTestClass, Method originalMethod) {
        int parameterCount = originalMethod.getParameterCount();
        if (parameterCount == 0) {
            return false;
        }
        Class<?> lastParam = originalMethod.getParameterTypes()[parameterCount - 1];
        return "kotlin.coroutines.Continuation".equals(lastParam.getName());
    }

    @Override
    public Object invoke(Object actualTestInstance, Method actualTestMethod, List<Object> actualTestMethodArgs,
            String testClassName) throws Throwable {
        ClassLoader methodClassLoader = actualTestMethod.getDeclaringClass().getClassLoader();

        Class<?> continuationClass = Class.forName("kotlin.coroutines.Continuation", false, methodClassLoader);

        var future = new CompletableFuture<Object>();

        var continuationProxy = Proxy.newProxyInstance(methodClassLoader,
                new Class<?>[] { continuationClass }, new ContinuationInvocationHandler(future, methodClassLoader));

        var invokeArgs = new ArrayList<Object>(actualTestMethodArgs);
        invokeArgs.add(continuationProxy);

        Object result;
        try {
            result = actualTestMethod.invoke(actualTestInstance, invokeArgs.toArray());
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }

        if (isCoroutineSuspended(result, methodClassLoader)) {
            try {
                return future.get();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw ex;
            } catch (ExecutionException ex) {
                throw ex.getCause();
            }
        }

        return result;
    }

    private static boolean isCoroutineSuspended(Object result, ClassLoader classLoader) {
        try {
            Class<?> intrinsicsClass = Class.forName("kotlin.coroutines.intrinsics.CoroutineSingletons", false,
                    classLoader);
            Field field = intrinsicsClass.getDeclaredField("COROUTINE_SUSPENDED");
            Object coroutineSuspended = field.get(null);
            return result == coroutineSuspended;
        } catch (Exception ex) {
            return false;
        }
    }

    private static final class ContinuationInvocationHandler implements InvocationHandler {

        private final CompletableFuture<Object> future;
        private final ClassLoader classLoader;

        ContinuationInvocationHandler(CompletableFuture<Object> future, ClassLoader classLoader) {
            this.future = future;
            this.classLoader = classLoader;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            int argCount = args == null ? 0 : args.length;
            if ("resumeWith".equals(methodName) && argCount == 1) {
                Object result = args[0];
                if (isKotlinResultFailure(result)) {
                    try {
                        Field exceptionField = result.getClass().getDeclaredField("exception");
                        exceptionField.setAccessible(true);
                        this.future.completeExceptionally((Throwable) exceptionField.get(result));
                    } catch (ReflectiveOperationException ex) {
                        this.future.completeExceptionally(ex);
                    }
                } else {
                    this.future.complete(result);
                }
                return null;
            }
            if ("getContext".equals(methodName) && argCount == 0) {
                Object dispatcher = tryLoadDefaultDispatcher(this.classLoader);
                if (dispatcher != null) {
                    return dispatcher;
                }
                try {
                    Class<?> emptyContextClass = Class.forName("kotlin.coroutines.EmptyCoroutineContext", false,
                            this.classLoader);
                    return emptyContextClass.getDeclaredField("INSTANCE").get(null);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
            throw new UnsupportedOperationException(methodName);
        }
    }

    private static boolean isKotlinResultFailure(Object result) {
        return result != null && KOTLIN_RESULT_FAILURE_CLASS_NAME.equals(result.getClass().getName());
    }

    private static Object tryLoadDefaultDispatcher(ClassLoader classLoader) {
        try {
            Class<?> dispatchersClass = Class.forName("kotlinx.coroutines.Dispatchers", false, classLoader);
            return dispatchersClass.getDeclaredField("Default").get(null);
        } catch (Exception ex) {
            return null;
        }
    }

}
