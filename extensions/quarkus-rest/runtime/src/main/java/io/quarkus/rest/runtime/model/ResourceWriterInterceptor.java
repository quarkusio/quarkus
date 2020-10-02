package io.quarkus.rest.runtime.model;

import java.util.Collections;
import java.util.Set;

import javax.ws.rs.Priorities;
import javax.ws.rs.ext.WriterInterceptor;

import io.quarkus.rest.runtime.spi.BeanFactory;

public class ResourceWriterInterceptor
        implements Comparable<ResourceWriterInterceptor>, SettableResourceInterceptor<WriterInterceptor>, HasPriority {

    private BeanFactory<WriterInterceptor> factory;
    private Integer priority = Priorities.USER; // default priority as defined by spec

    /**
     * The class names of the {@code @NameBinding} annotations that the method is annotated with.
     */
    private Set<String> nameBindingNames = Collections.emptySet();

    public void setFactory(BeanFactory<WriterInterceptor> factory) {
        this.factory = factory;
    }

    public BeanFactory<WriterInterceptor> getFactory() {
        return factory;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Set<String> getNameBindingNames() {
        return nameBindingNames;
    }

    public void setNameBindingNames(Set<String> nameBindingNames) {
        this.nameBindingNames = nameBindingNames;
    }

    // spec says that request interceptors are sorted in ascending order
    @Override
    public int compareTo(ResourceWriterInterceptor o) {
        return this.priority.compareTo(o.priority);
    }
}
