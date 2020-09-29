package io.quarkus.rest.runtime.jaxrs;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Priority;
import javax.ws.rs.Consumes;
import javax.ws.rs.Priorities;
import javax.ws.rs.Produces;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.RuntimeDelegate;
import javax.ws.rs.ext.WriterInterceptor;

import io.quarkus.rest.runtime.core.Serialisers;
import io.quarkus.rest.runtime.core.UnmanagedBeanFactory;
import io.quarkus.rest.runtime.model.ResourceReader;
import io.quarkus.rest.runtime.model.ResourceWriter;
import io.quarkus.rest.runtime.util.MultivaluedTreeMap;
import io.quarkus.rest.runtime.util.Types;

public class QuarkusRestConfiguration implements Configuration {

    private final RuntimeType runtimeType;
    private final Map<String, Object> properties;
    private final Map<Class<?>, Object> allInstances;
    private final List<Feature> enabledFeatures;
    private final MultivaluedMap<Integer, ClientRequestFilter> requestFilters;
    private final MultivaluedMap<Integer, ClientResponseFilter> responseFilters;
    private final MultivaluedMap<Integer, WriterInterceptor> writerInterceptors;
    private final MultivaluedMap<Integer, ReaderInterceptor> readerInterceptors;
    private final MultivaluedMap<Class<?>, ResourceWriter> resourceWriters;
    private final MultivaluedMap<Class<?>, ResourceReader> resourceReaders;

    public QuarkusRestConfiguration(RuntimeType runtimeType) {
        this.runtimeType = runtimeType;
        this.properties = new HashMap<>();
        this.allInstances = new HashMap<>();
        this.enabledFeatures = new ArrayList<>();
        this.requestFilters = new MultivaluedTreeMap<>();
        this.responseFilters = new MultivaluedTreeMap<>(Collections.reverseOrder());
        this.readerInterceptors = new MultivaluedTreeMap<>();
        this.writerInterceptors = new MultivaluedTreeMap<>(Collections.reverseOrder());
        this.resourceReaders = new MultivaluedHashMap<>();
        this.resourceWriters = new MultivaluedHashMap<>();
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
            this.readerInterceptors = new MultivaluedTreeMap<>();
            this.readerInterceptors.putAll(quarkusRestConfiguration.readerInterceptors);
            this.writerInterceptors = new MultivaluedTreeMap<>(Collections.reverseOrder());
            this.writerInterceptors.putAll(quarkusRestConfiguration.writerInterceptors);
            this.resourceReaders = new MultivaluedHashMap<>();
            this.resourceReaders.putAll(quarkusRestConfiguration.resourceReaders);
            this.resourceWriters = new MultivaluedHashMap<>();
            this.resourceWriters.putAll(quarkusRestConfiguration.resourceWriters);
        } else {
            this.allInstances = new HashMap<>();
            this.enabledFeatures = new ArrayList<>();
            this.requestFilters = new MultivaluedTreeMap<>();
            this.responseFilters = new MultivaluedTreeMap<>(
                    Collections.reverseOrder());
            this.readerInterceptors = new MultivaluedTreeMap<>();
            this.writerInterceptors = new MultivaluedTreeMap<>(Collections.reverseOrder());
            this.resourceReaders = new MultivaluedHashMap<>();
            this.resourceWriters = new MultivaluedHashMap<>();
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
        if (component instanceof WriterInterceptor) {
            int effectivePriority = priority != null ? priority : determinePriority(component);
            writerInterceptors.add(effectivePriority, (WriterInterceptor) component);
        }
        if (component instanceof ReaderInterceptor) {
            int effectivePriority = priority != null ? priority : determinePriority(component);
            readerInterceptors.add(effectivePriority, (ReaderInterceptor) component);
        }
        if (component instanceof MessageBodyReader) {
            ResourceReader resourceReader = new ResourceReader();
            resourceReader.setFactory(new UnmanagedBeanFactory(component));
            Consumes consumes = component.getClass().getAnnotation(Consumes.class);
            resourceReader
                    .setMediaTypeStrings(consumes != null ? Arrays.asList(consumes.value()) : Serialisers.WILDCARD_STRING_LIST);
            Type[] args = Types.findParameterizedTypes(component.getClass(), MessageBodyReader.class);
            resourceReaders.add(args != null && args.length == 1 ? Types.getRawType(args[0]) : Object.class, resourceReader);
        }
        if (component instanceof MessageBodyWriter) {
            ResourceWriter resourceWriter = new ResourceWriter();
            resourceWriter.setFactory(new UnmanagedBeanFactory(component));
            Produces produces = component.getClass().getAnnotation(Produces.class);
            resourceWriter
                    .setMediaTypeStrings(produces != null ? Arrays.asList(produces.value()) : Serialisers.WILDCARD_STRING_LIST);
            Type[] args = Types.findParameterizedTypes(component.getClass(), MessageBodyWriter.class);
            resourceWriters.add(args != null && args.length == 1 ? Types.getRawType(args[0]) : Object.class, resourceWriter);
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

    public List<WriterInterceptor> getWriterInterceptors() {
        if (writerInterceptors.isEmpty()) {
            return Collections.emptyList();
        }
        List<WriterInterceptor> result = new ArrayList<>(writerInterceptors.size() * 2);
        for (List<WriterInterceptor> writerInterceptors : writerInterceptors.values()) {
            result.addAll(writerInterceptors);
        }
        return result;
    }

    public List<ReaderInterceptor> getReaderInterceptors() {
        if (readerInterceptors.isEmpty()) {
            return Collections.emptyList();
        }
        List<ReaderInterceptor> result = new ArrayList<>(readerInterceptors.size() * 2);
        for (List<ReaderInterceptor> readerInterceptors : readerInterceptors.values()) {
            result.addAll(readerInterceptors);
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

    public MultivaluedMap<Class<?>, ResourceReader> getResourceReaders() {
        return resourceReaders;
    }

    public MultivaluedMap<Class<?>, ResourceWriter> getResourceWriters() {
        return resourceWriters;
    }
}
