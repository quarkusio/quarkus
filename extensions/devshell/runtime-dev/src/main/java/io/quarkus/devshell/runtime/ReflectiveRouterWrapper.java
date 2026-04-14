package io.quarkus.devshell.runtime;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

/**
 * A wrapper around DevShellRouter that uses reflection to call methods.
 * This is needed because the router instance may be from a different classloader
 * than the runtime-dev classes.
 */
public class ReflectiveRouterWrapper extends DevShellRouter {

    private static final Logger LOG = Logger.getLogger(ReflectiveRouterWrapper.class);

    private final Object delegate;
    private final ClassLoader delegateClassLoader;
    private Method callMethod;
    private Method subscribeMethod;
    private Method unsubscribeMethod;
    private Method getRuntimeMethodsMethod;
    private Method getDeploymentMethodsMethod;

    public ReflectiveRouterWrapper(Object delegate) {
        this.delegate = delegate;
        this.delegateClassLoader = delegate.getClass().getClassLoader();
        cacheMethods();
    }

    private void cacheMethods() {
        try {
            Class<?> delegateClass = delegate.getClass();
            callMethod = delegateClass.getMethod("call", String.class, Map.class);
            getRuntimeMethodsMethod = delegateClass.getMethod("getRuntimeMethods");
            getDeploymentMethodsMethod = delegateClass.getMethod("getDeploymentMethods");

            // Subscribe/unsubscribe are optional - streaming may not be supported
            try {
                subscribeMethod = delegateClass.getMethod("subscribe", String.class, Map.class,
                        Consumer.class, Consumer.class, Runnable.class);
                unsubscribeMethod = delegateClass.getMethod("unsubscribe", int.class);
            } catch (NoSuchMethodException e) {
                // Streaming not supported, leave methods as null
                subscribeMethod = null;
                unsubscribeMethod = null;
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Failed to find router methods", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<String> call(String methodName, Map<String, Object> params) {
        // Save and restore thread context classloader to ensure Kafka and other
        // libraries can load classes correctly across classloader boundaries
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(delegateClassLoader);
            return (CompletableFuture<String>) callMethod.invoke(delegate, methodName, params);
        } catch (Exception e) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    @Override
    public int subscribe(String method, Map<String, Object> params,
            Consumer<String> onMessage, Consumer<Throwable> onError, Runnable onComplete) {
        if (subscribeMethod == null) {
            onError.accept(new UnsupportedOperationException("Streaming not supported"));
            return -1;
        }
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(delegateClassLoader);
            return (int) subscribeMethod.invoke(delegate, method, params, onMessage, onError, onComplete);
        } catch (Exception e) {
            onError.accept(e);
            return -1;
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    @Override
    public void unsubscribe(int subscriptionId) {
        if (unsubscribeMethod == null) {
            return;
        }
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(delegateClassLoader);
            unsubscribeMethod.invoke(delegate, subscriptionId);
        } catch (Exception e) {
            LOG.debugf(e, "Failed to unsubscribe: %d", subscriptionId);
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, ?> getRuntimeMethods() {
        try {
            return (Map<String, ?>) getRuntimeMethodsMethod.invoke(delegate);
        } catch (Exception e) {
            LOG.debugf(e, "Failed to get runtime methods");
            return Map.of();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, ?> getDeploymentMethods() {
        try {
            return (Map<String, ?>) getDeploymentMethodsMethod.invoke(delegate);
        } catch (Exception e) {
            LOG.debugf(e, "Failed to get deployment methods");
            return Map.of();
        }
    }
}
