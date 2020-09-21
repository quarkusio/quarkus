package io.quarkus.rest.runtime.jaxrs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.ext.RuntimeDelegate;

public class QuarkusRestConfiguration implements Configuration {

    private final RuntimeType runtimeType;
    private final Map<String, Object> properties = new HashMap<>();
    private final Map<Class<?>, Object> allInstances = new HashMap<>();
    private final List<Feature> enabledFeatures = new ArrayList<>();
    private final List<ClientRequestFilter> requestFilters = new ArrayList<>();
    private final List<ClientResponseFilter> responseFilters = new ArrayList<>();

    public QuarkusRestConfiguration(RuntimeType runtimeType) {
        this.runtimeType = runtimeType;
    }

    public QuarkusRestConfiguration(Configuration configuration) {
        this.runtimeType = configuration.getRuntimeType();
        this.properties.putAll(configuration.getProperties());
        for (Object i : configuration.getInstances()) {
            register(i);
        }
    }

    @Override
    public RuntimeType getRuntimeType() {
        return runtimeType;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public Object getProperty(String name) {
        return properties.get(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return properties.keySet();
    }

    @Override
    public boolean isEnabled(Feature feature) {
        return enabledFeatures.contains(feature);
    }

    @Override
    public boolean isEnabled(Class<? extends Feature> featureClass) {
        for (Feature enabledFeature : enabledFeatures) {
            if (enabledFeature.getClass().equals(featureClass)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isRegistered(Object component) {
        return allInstances.get(component.getClass()) == component;
    }

    @Override
    public boolean isRegistered(Class<?> componentClass) {
        return allInstances.containsKey(componentClass);
    }

    @Override
    public Map<Class<?>, Integer> getContracts(Class<?> componentClass) {
        return Collections.emptyMap();
    }

    @Override
    public Set<Class<?>> getClasses() {
        return new HashSet<>(allInstances.keySet());
    }

    @Override
    public Set<Object> getInstances() {
        return new HashSet<>(allInstances.values());
    }

    public void addEnabledFeature(Feature feature) {
        enabledFeatures.add(feature);
    }

    public String toString(Object value) {
        return value.toString();
    }

    @SuppressWarnings("unchecked")
    public String toHeaderString(Object obj) {
        if (obj instanceof String) {
            return (String) obj;
        } else {
            // TODO: we probably want a more direct way to get the delegate instead of going through all the indirection
            return RuntimeDelegate.getInstance().createHeaderDelegate((Class<Object>) obj.getClass()).toString(obj);
        }
    }

    public void property(String name, Object value) {
        properties.put(name, value);
    }

    public void register(Class<?> componentClass, int priority) {
        try {
            register(componentClass.newInstance());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void register(Class<?> componentClass, Class<?>... contracts) {
        try {
            register(componentClass.newInstance());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void register(Object component) {
        allInstances.put(component.getClass(), component);
        if (component instanceof Feature) {
            enabledFeatures.add((Feature) component);
        }
        if (component instanceof ClientRequestFilter) {
            requestFilters.add((ClientRequestFilter) component);
        }
        if (component instanceof ClientResponseFilter) {
            responseFilters.add((ClientResponseFilter) component);
        }
    }

    public void register(Object component, Class<?>[] contracts) {
        register(component);
    }

    public void register(Object component, Map<Class<?>, Integer> contracts) {
        register(component);

    }

    public void register(Object component, int priority) {
        register(component);
    }

    public List<ClientRequestFilter> getRequestFilters() {
        return requestFilters;
    }

    public List<ClientResponseFilter> getResponseFilters() {
        return responseFilters;
    }
}
