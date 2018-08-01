package org.jboss.shamrock.weld.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WeldDeployment {

    private final List<Class<?>> additionalBeans = new ArrayList<>();
    private final List<Class<?>> interceptors = new ArrayList<>();

    public void addAdditionalBean(Class<?> ... beanClass) {
        additionalBeans.addAll(Arrays.asList(beanClass));
    }

    List<Class<?>> getAdditionalBeans() {
        return additionalBeans;
    }

    public void addInterceptor(Class<?> interceptorClass) {
        this.interceptors.add( interceptorClass);
    }

    List<Class<?>> getInterceptors() {
        return this.interceptors;
    }

}
