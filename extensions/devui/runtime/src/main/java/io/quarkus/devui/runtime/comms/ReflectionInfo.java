package io.quarkus.devui.runtime.comms;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Contains reflection info on the beans that needs to be called from the jsonrpc router
 */
public class ReflectionInfo {
    private final boolean blocking;
    private final boolean nonBlocking;
    public Class bean;
    public Object instance;
    public Method method;
    public Map<String, Class> params;

    public ReflectionInfo(Class bean, Object instance, Method method, Map<String, Class> params, boolean explicitlyBlocking,
            boolean explicitlyNonBlocking) {
        this.bean = bean;
        this.instance = instance;
        this.method = method;
        this.params = params;
        this.blocking = explicitlyBlocking;
        this.nonBlocking = explicitlyNonBlocking;
        if (blocking && nonBlocking) {
            throw new IllegalArgumentException("The method " + method.getDeclaringClass().getName() + "." + method.getName()
                    + " cannot be annotated with @Blocking and @NonBlocking");
        }
    }

    public boolean isReturningMulti() {
        Class<?> returnType = this.method.getReturnType();
        return returnType.getName().equals(Multi.class.getName());
    }

    public boolean isExplicitlyBlocking() {
        return blocking;
    }

    public boolean isExplicitlyNonBlocking() {
        return nonBlocking;
    }

    public boolean isReturningUni() {
        return method.getReturnType().getName().equals(Uni.class.getName());
    }

    public boolean isReturningCompletionStage() {
        return method.getReturnType().getName().equals(CompletionStage.class.getName()) ||
                method.getReturnType().getName().equals(CompletableFuture.class.getName());
    }
}
