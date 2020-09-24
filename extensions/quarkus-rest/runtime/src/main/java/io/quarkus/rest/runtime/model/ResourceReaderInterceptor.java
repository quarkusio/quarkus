package io.quarkus.rest.runtime.model;

import java.util.Collections;
import java.util.Set;

import javax.ws.rs.Priorities;
import javax.ws.rs.ext.ReaderInterceptor;

import io.quarkus.rest.runtime.spi.BeanFactory;

public class ResourceReaderInterceptor
        implements Comparable<ResourceReaderInterceptor>, SettableResourceInterceptor<ReaderInterceptor> {

    private BeanFactory<ReaderInterceptor> factory;
    private boolean preMatching;
    private Integer priority = Priorities.USER; // default priority as defined by spec

    /**
     * The class names of the {@code @NameBinding} annotations that the method is annotated with.
     */
    private Set<String> nameBindingNames = Collections.emptySet();

    public void setFactory(BeanFactory<ReaderInterceptor> factory) {
        this.factory = factory;
    }

    public BeanFactory<ReaderInterceptor> getFactory() {
        return factory;
    }

    public void setPreMatching(boolean preMatching) {
        this.preMatching = preMatching;
    }

    public boolean isPreMatching() {
        return preMatching;
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
    public int compareTo(ResourceReaderInterceptor o) {
        return this.priority.compareTo(o.priority);
    }
}
