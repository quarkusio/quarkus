package io.quarkus.rest.runtime.jaxrs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.RuntimeDelegate;

import io.quarkus.rest.runtime.util.MultivaluedTreeMap;

public class QuarkusRestConfiguration implements Configuration {

    private final RuntimeType runtimeType;
    private final Map<String, Object> properties;
    private final Map<Class<?>, Object> allInstances;
    private final List<Feature> enabledFeatures;
    private final MultivaluedMap<Integer, ClientRequestFilter> requestFilters;
    private final MultivaluedMap<Integer, ClientResponseFilter> responseFilters;

    public QuarkusRestConfiguration(RuntimeType runtimeType) {
        this.runtimeType = runtimeType;
        this.properties = new HashMap<>();
        this.allInstances = new HashMap<>();
        this.enabledFeatures = new ArrayList<>();
        this.requestFilters = new MultivaluedTreeMap<>();
        this.responseFilters = new MultivaluedTreeMap<>(Collections.reverseOrder());
    }

    public QuarkusRestConfiguration(Configuration configuration) {
        this.runtimeType = configuration.getRuntimeType();
        this.properties = new HashMap<>(configuration.getProperties());
        if (configuration instanceof QuarkusRestConfiguration) {
            // we want to preserve all the registration metadata
            QuarkusRestConfiguration quarkusRestConfiguration = (QuarkusRestConfiguration) configuration;
            this.enabledFeatures = new ArrayList<>(quarkusRestConfiguration.enabledFeatures);
            this.allInstances = new HashMap<>(quarkusRestConfiguration.allInstances);
            this.requestFilters = new MultivaluedTreeMap<>();
            this.requestFilters.putAll(quarkusRestConfiguration.requestFilters);
            this.responseFilters = new MultivaluedTreeMap<>(Collections.reverseOrder());
            this.responseFilters.putAll(quarkusRestConfiguration.responseFilters);
        } else {
            this.allInstances = new HashMap<>();
            this.enabledFeatures = new ArrayList<>();
            this.requestFilters = new MultivaluedTreeMap<>();
            this.responseFilters = new MultivaluedTreeMap<>(
                    Collections.reverseOrder());
            // this is the best we can do - we don't have any of the metadata associated with the registration
            for (Object i : configuration.getInstances()) {
                register(i);
            }
        }
    }

    @Override
    public RuntimeType getRuntimeType() {
        return runtimeType;
    }

    @Override
    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
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

    public void register(Class<?> componentClass) {
        try {
            register(componentClass.getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void register(Class<?> componentClass, int priority) {
        try {
            register(componentClass.getDeclaredConstructor().newInstance(), priority);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void register(Class<?> componentClass, Class<?>... contracts) {
        try {
            register(componentClass.getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void register(Object component) {
        register(component, (Integer) null);
    }

    private void register(Object component, Integer priority) {
        allInstances.put(component.getClass(), component);
        if (component instanceof Feature) {
            enabledFeatures.add((Feature) component);
        }
        if (component instanceof ClientRequestFilter) {
            int effectivePriority = priority != null ? priority : determinePriority(component);
            requestFilters.add(effectivePriority, (ClientRequestFilter) component);
        }
        if (component instanceof ClientResponseFilter) {
            int effectivePriority = priority != null ? priority : determinePriority(component);
            responseFilters.add(effectivePriority, (ClientResponseFilter) component);
        }
    }

    public void register(Object component, Class<?>[] contracts) {
        register(component);
    }

    public void register(Object component, Map<Class<?>, Integer> contracts) {
        register(component);

    }

    public void register(Object component, int priority) {
        register(component, Integer.valueOf(priority));
    }

    public List<ClientRequestFilter> getRequestFilters() {
        if (requestFilters.isEmpty()) {
            return Collections.emptyList();
        }
        List<ClientRequestFilter> result = new ArrayList<>(requestFilters.size() * 2);
        for (List<ClientRequestFilter> requestFilters : requestFilters.values()) {
            result.addAll(requestFilters);
        }
        return result;
    }

    public List<ClientResponseFilter> getResponseFilters() {
        if (responseFilters.isEmpty()) {
            return Collections.emptyList();
        }
        List<ClientResponseFilter> result = new ArrayList<>(responseFilters.size() * 2);
        for (List<ClientResponseFilter> responseFilters : responseFilters.values()) {
            result.addAll(responseFilters);
        }
        return result;
    }

    // TODO: we could generate some kind of index at build time in order to obtain these values without using the annotation
    private int determinePriority(Object object) {
        Priority priority = object.getClass().getDeclaredAnnotation(Priority.class);
        if (priority == null) {
            return Priorities.USER;
        }
        return priority.value();
    }
}
