package io.quarkus.arc.deployment.devconsole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DevBeanInfos {

    private final List<DevBeanInfo> beans;
    private final List<DevBeanInfo> removedBeans;
    private final List<DevObserverInfo> observers;
    private final List<DevInterceptorInfo> interceptors;
    private final List<DevInterceptorInfo> removedInterceptors;
    private final List<DevDecoratorInfo> decorators;
    private final List<DevDecoratorInfo> removedDecorators;

    public DevBeanInfos() {
        beans = new ArrayList<>();
        removedBeans = new ArrayList<>();
        observers = new ArrayList<>();
        interceptors = new ArrayList<>();
        removedInterceptors = new ArrayList<>();
        decorators = new ArrayList<>();
        removedDecorators = new ArrayList<>();
    }

    public List<DevBeanInfo> getRemovedBeans() {
        return removedBeans;
    }

    public List<DevBeanInfo> getBeans() {
        return beans;
    }

    public List<DevObserverInfo> getObservers() {
        return observers;
    }

    public List<DevInterceptorInfo> getInterceptors() {
        return interceptors;
    }

    public List<DevDecoratorInfo> getDecorators() {
        return decorators;
    }

    public List<DevInterceptorInfo> getRemovedInterceptors() {
        return removedInterceptors;
    }

    public List<DevDecoratorInfo> getRemovedDecorators() {
        return removedDecorators;
    }

    public DevInterceptorInfo getInterceptor(String id) {
        for (DevInterceptorInfo interceptor : interceptors) {
            if (interceptor.getId().equals(id)) {
                return interceptor;
            }
        }
        return null;
    }

    public int getRemovedComponents() {
        return removedBeans.size() + removedInterceptors.size() + removedDecorators.size();
    }

    void addBean(DevBeanInfo beanInfo) {
        beans.add(beanInfo);
    }

    void addRemovedBean(DevBeanInfo beanInfo) {
        removedBeans.add(beanInfo);
    }

    void addObserver(DevObserverInfo observer) {
        observers.add(observer);
    }

    void addInterceptor(DevInterceptorInfo interceptor) {
        interceptors.add(interceptor);
    }

    void addRemovedInterceptor(DevInterceptorInfo interceptor) {
        removedInterceptors.add(interceptor);
    }

    void addDecorator(DevDecoratorInfo decorator) {
        decorators.add(decorator);
    }

    void addRemovedDecorator(DevDecoratorInfo decorator) {
        removedDecorators.add(decorator);
    }

    void sort() {
        Collections.sort(beans);
        Collections.sort(removedBeans);
        Collections.sort(observers);
        Collections.sort(interceptors);
        Collections.sort(decorators);
        Collections.sort(removedDecorators);
        Collections.sort(removedInterceptors);
    }
}
