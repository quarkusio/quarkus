package io.quarkus.devui.runtime.comms;

import java.lang.reflect.Method;
import java.util.Map;

import io.smallrye.mutiny.Multi;

/**
 * Contains reflection info on the beans that needs to be called from the jsonrpc router
 */
public class ReflectionInfo {
    public Class bean;
    public Object instance;
    public Method method;
    public Map<String, Class> params;

    public ReflectionInfo(Class bean, Object instance, Method method, Map<String, Class> params) {
        this.bean = bean;
        this.instance = instance;
        this.method = method;
        this.params = params;
    }

    public boolean isSubscription() {
        Class<?> returnType = this.method.getReturnType();
        return returnType.getName().equals(Multi.class.getName());
    }
}
