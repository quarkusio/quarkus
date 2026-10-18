package io.quarkus.arc.deployment.devui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.dev.console.DevConsoleManager;

public class DevBeanInfos {

    private final List<DevBeanInfo> beans;
    private final List<DevBeanInfo> removedBeans;
    private final List<DevObserverInfo> observers;
    private final List<DevInterceptorInfo> interceptors;
    private final List<DevInterceptorInfo> removedInterceptors;
    private final List<DevDecoratorInfo> decorators;
    private final List<DevDecoratorInfo> removedDecorators;
    private final Map<String, DependencyGraph> dependencyGraphs;

    public DevBeanInfos() {
        beans = new ArrayList<>();
        removedBeans = new ArrayList<>();
        observers = new ArrayList<>();
        interceptors = new ArrayList<>();
        removedInterceptors = new ArrayList<>();
        decorators = new ArrayList<>();
        removedDecorators = new ArrayList<>();
        dependencyGraphs = new HashMap<>();
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

    public Map<String, DependencyGraph> getDependencyGraphs() {
        return dependencyGraphs;
    }

    public String getBeanDescription() {
        return DevConsoleManager.getGlobal(BEAN_DESCRIPTION);
    }

    public int getMaxDependencyLevel() {
        Integer val = DevConsoleManager.getGlobal(MAX_DEPENDENCY_LEVEL);
        return val != null ? val : DEFAULT_MAX_DEPENDENCY_LEVEL;
    }

    public DevBeanInfo getBean(String id) {
        for (DevBeanInfo bean : beans) {
            if (bean.getId().equals(id)) {
                return bean;
            }
        }
        return null;
    }

    public DevInterceptorInfo getInterceptor(String id) {
        for (DevInterceptorInfo interceptor : interceptors) {
            if (interceptor.getId().equals(id)) {
                return interceptor;
            }
        }
        return null;
    }

    public DependencyGraph getDependencyGraph(String beanId) {
        // Note that MAX_DEPENDENCY_LEVEL is not implemented in UI yet
        Integer maxLevel = DevConsoleManager.getGlobal(MAX_DEPENDENCY_LEVEL);
        if (maxLevel == null) {
            maxLevel = DEFAULT_MAX_DEPENDENCY_LEVEL;
        }
        if (dependencyGraphs.isEmpty()) {
            return DependencyGraph.EMPTY;
        }
        DependencyGraph graph = dependencyGraphs.get(beanId);
        return graph.maxLevel <= maxLevel ? graph : graph.forLevel(maxLevel);
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

    void addDependencyGraph(String beanId, DependencyGraph graph) {
        dependencyGraphs.put(beanId, graph);
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

    static final String BEAN_DESCRIPTION = "io.quarkus.arc.beanDescription";
    static final String MAX_DEPENDENCY_LEVEL = "io.quarkus.arc.maxDependencyLevel";
    public static final String BEAN_DEPENDENCIES = "io.quarkus.arc.beanDependencies";
    static final int DEFAULT_MAX_DEPENDENCY_LEVEL = 10;
}
