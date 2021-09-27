package io.quarkus.arc.deployment.devconsole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DevBeanInfos {

    private final List<DevBeanInfo> beans;
    private final List<DevBeanInfo> removedBeans;
    private final List<DevObserverInfo> observers;
    private final List<DevInterceptorInfo> interceptors;

    public DevBeanInfos() {
        beans = new ArrayList<>();
        removedBeans = new ArrayList<>();
        observers = new ArrayList<>();
        interceptors = new ArrayList<>();
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

    public DevInterceptorInfo getInterceptor(String id) {
        for (DevInterceptorInfo interceptor : interceptors) {
            if (interceptor.getId().equals(id)) {
                return interceptor;
            }
        }
        return null;
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

    void sort() {
        Collections.sort(beans);
        Collections.sort(removedBeans);
        Collections.sort(observers);
        Collections.sort(interceptors);
    }
}
