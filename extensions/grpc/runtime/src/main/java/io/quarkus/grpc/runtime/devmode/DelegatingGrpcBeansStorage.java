package io.quarkus.grpc.runtime.devmode;

import java.util.HashMap;
import java.util.Map;

public abstract class DelegatingGrpcBeansStorage {

    private final Map<String, String> userClassesByGeneratedBean = new HashMap<>();

    @SuppressWarnings("unused") // used from generated code
    public void addDelegatingMapping(String userClassName, String delegatingBeanName) {
        userClassesByGeneratedBean.put(delegatingBeanName, userClassName);
    }

    public String getUserClassName(String delegatingBeanName) {
        return userClassesByGeneratedBean.get(delegatingBeanName);
    }
}
