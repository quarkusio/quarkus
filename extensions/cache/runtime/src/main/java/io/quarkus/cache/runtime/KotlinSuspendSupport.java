package io.quarkus.cache.runtime;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import jakarta.interceptor.InvocationContext;

import io.quarkus.cache.CacheException;
import io.smallrye.mutiny.Uni;

/**
 * Minimal bridge for {@code @CacheResult} on Kotlin {@code suspend} functions.
 * Uses reflection so {@code quarkus-cache} does not depend on Kotlin.
 */
final class KotlinSuspendSupport {

    private static final String CONTINUATION_CLASS_NAME = "kotlin.coroutines.Continuation";

    private static volatile KotlinRuntime runtime;

    private KotlinSuspendSupport() {
    }

    static boolean isSuspendedFunction(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        return parameterTypes.length > 0
                && CONTINUATION_CLASS_NAME.equals(parameterTypes[parameterTypes.length - 1].getName());
    }

    static Uni<Object> invokeSuspendedAsUni(InvocationContext invocationContext) {
        KotlinRuntime kt = runtime();
        return Uni.createFrom().emitter(emitter -> {
            Object[] parameters = invocationContext.getParameters();
            int continuationIndex = parameters.length - 1;
            Object originalContinuation = parameters[continuationIndex];
            AtomicBoolean completed = new AtomicBoolean(false);

            Object proxyContinuation = Proxy.newProxyInstance(
                    kt.continuationClass.getClassLoader(),
                    new Class<?>[] { kt.continuationClass },
                    new ContinuationHandler(kt, originalContinuation, (value, error) -> {
                        if (completed.compareAndSet(false, true)) {
                            if (error != null) {
                                emitter.fail(error);
                            } else {
                                emitter.complete(value);
                            }
                        }
                    }));

            Object[] newParameters = parameters.clone();
            newParameters[continuationIndex] = proxyContinuation;
            invocationContext.setParameters(newParameters);

            try {
                Object result = invocationContext.proceed();
                if (result != kt.coroutineSuspended) {
                    if (completed.compareAndSet(false, true)) {
                        emitter.complete(result);
                    }
                }
            } catch (Exception e) {
                if (completed.compareAndSet(false, true)) {
                    emitter.fail(e);
                }
            }
        });
    }

    static Object resumeContinuationFromUni(InvocationContext invocationContext, Uni<?> uni) {
        KotlinRuntime kt = runtime();
        Object continuation = invocationContext.getParameters()[invocationContext.getParameters().length - 1];
        uni.subscribe().with(
                item -> resume(kt, continuation, item, null),
                failure -> resume(kt, continuation, null, failure));
        return kt.coroutineSuspended;
    }

    private static void resume(KotlinRuntime kt, Object continuation, Object value, Throwable error) {
        try {
            Object result = error != null ? kt.createFailure.invoke(null, unwrap(error)) : value;
            kt.resumeWith.invoke(continuation, result);
        } catch (ReflectiveOperationException e) {
            throw new CacheException("Failed to resume Kotlin Continuation", e);
        }
    }

    private static Throwable unwrap(Throwable error) {
        if (error instanceof CacheException && error.getCause() != null) {
            return error.getCause();
        }
        return error;
    }

    private static KotlinRuntime runtime() {
        KotlinRuntime existing = runtime;
        if (existing != null) {
            return existing;
        }
        synchronized (KotlinSuspendSupport.class) {
            existing = runtime;
            if (existing != null) {
                return existing;
            }
            existing = KotlinRuntime.load();
            runtime = existing;
            return existing;
        }
    }

    private static final class KotlinRuntime {
        final Class<?> continuationClass;
        final Object coroutineSuspended;
        final Method throwOnFailure;
        final Method resumeWith;
        final Method getContext;
        final Method createFailure;

        private KotlinRuntime(Class<?> continuationClass, Object coroutineSuspended, Method throwOnFailure,
                Method resumeWith, Method getContext, Method createFailure) {
            this.continuationClass = continuationClass;
            this.coroutineSuspended = coroutineSuspended;
            this.throwOnFailure = throwOnFailure;
            this.resumeWith = resumeWith;
            this.getContext = getContext;
            this.createFailure = createFailure;
        }

        static KotlinRuntime load() {
            try {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                if (cl == null) {
                    cl = KotlinSuspendSupport.class.getClassLoader();
                }
                Class<?> continuationClass = Class.forName(CONTINUATION_CLASS_NAME, false, cl);
                Class<?> intrinsics = Class.forName("kotlin.coroutines.intrinsics.IntrinsicsKt", false, cl);
                Method getCoroutineSuspended = intrinsics.getMethod("getCOROUTINE_SUSPENDED");
                getCoroutineSuspended.setAccessible(true);
                Object suspended = getCoroutineSuspended.invoke(null);
                Class<?> resultKt = Class.forName("kotlin.ResultKt", false, cl);
                Method throwOnFailure = resultKt.getMethod("throwOnFailure", Object.class);
                throwOnFailure.setAccessible(true);
                Method createFailure = resultKt.getMethod("createFailure", Throwable.class);
                createFailure.setAccessible(true);
                Method resumeWith = continuationClass.getMethod("resumeWith", Object.class);
                resumeWith.setAccessible(true);
                Method getContext = continuationClass.getMethod("getContext");
                getContext.setAccessible(true);
                return new KotlinRuntime(continuationClass, suspended, throwOnFailure, resumeWith, getContext,
                        createFailure);
            } catch (ReflectiveOperationException e) {
                throw new CacheException(
                        "Kotlin suspend function support requires kotlin-stdlib on the classpath", e);
            }
        }
    }

    private static final class ContinuationHandler implements InvocationHandler {

        private final KotlinRuntime kt;
        private final Object originalContinuation;
        private final BiConsumer<Object, Throwable> onResume;

        ContinuationHandler(KotlinRuntime kt, Object originalContinuation, BiConsumer<Object, Throwable> onResume) {
            this.kt = kt;
            this.originalContinuation = originalContinuation;
            this.onResume = onResume;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("resumeWith".equals(name) && args != null && args.length == 1) {
                Object result = args[0];
                try {
                    kt.throwOnFailure.invoke(null, result);
                    onResume.accept(result, null);
                } catch (InvocationTargetException e) {
                    onResume.accept(null, e.getCause() != null ? e.getCause() : e);
                }
                return null;
            }
            if ("getContext".equals(name)) {
                return kt.getContext.invoke(originalContinuation);
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("toString".equals(name)) {
                return "CacheContinuationProxy";
            }
            return method.invoke(originalContinuation, args);
        }
    }
}
