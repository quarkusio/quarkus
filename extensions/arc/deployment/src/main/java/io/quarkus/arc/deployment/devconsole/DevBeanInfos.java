package io.quarkus.arc.deployment.devconsole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DevBeanInfos {

    private final List<DevBeanInfo> beans;
    private final List<DevBeanInfo> removedBeans;
    private final List<DevObserverInfo> observers;

    public DevBeanInfos() {
        beans = new ArrayList<>();
        removedBeans = new ArrayList<>();
        observers = new ArrayList<>();
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

    void addBean(DevBeanInfo beanInfo) {
        beans.add(beanInfo);
    }

    void addRemovedBean(DevBeanInfo beanInfo) {
        removedBeans.add(beanInfo);
    }

    void addObserver(DevObserverInfo observer) {
        observers.add(observer);
    }

    void sort() {
        Collections.sort(beans);
        Collections.sort(removedBeans);
        Collections.sort(observers);
    }
}
