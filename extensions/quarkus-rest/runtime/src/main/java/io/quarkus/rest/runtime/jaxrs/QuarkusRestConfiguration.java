package io.quarkus.rest.runtime.jaxrs;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;

public class QuarkusRestConfiguration implements Configuration {

    private final RuntimeType runtimeType;
    private final Map<String, Object> properties = new HashMap<>();
    private final Set<Feature> enabledFeatures = new HashSet<>();

    public QuarkusRestConfiguration(RuntimeType runtimeType) {
        this.runtimeType = runtimeType;
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
        return false;
    }

    @Override
    public boolean isRegistered(Class<?> componentClass) {
        return false;
    }

    @Override
    public Map<Class<?>, Integer> getContracts(Class<?> componentClass) {
        return null;
    }

    @Override
    public Set<Class<?>> getClasses() {
        return null;
    }

    @Override
    public Set<Object> getInstances() {
        return null;
    }

    public void addEnabledFeature(Feature feature) {
        enabledFeatures.add(feature);
    }

    public String toString(Object value) {
        return value.toString();
    }

    public String toHeaderString(Object obj) {
        if (obj instanceof String) {
            return (String) obj;
        } else {
            //TODO: fixme
            return new QuarkusRestRuntimeDelegate().createHeaderDelegate((Class<Object>) obj.getClass()).toString(obj);
        }
    }

    public void property(String name, Object value) {
        properties.put(name, value);
    }

    public void register(Class<?> componentClass, int priority) {

    }

    public void register(Class<?> componentClass, Class<?>... contracts) {

    }

    public void register(Object component) {

    }

    public void register(Object component, Class<?>[] contracts) {

    }

    public void register(Object component, Map<Class<?>, Integer> contracts) {

    }

    public void register(Object component, int priority) {

    }
}
