package io.quarkus.rest.runtime.model;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestFilter;

import io.quarkus.rest.runtime.spi.BeanFactory;

public class ResourceRequestInterceptor implements Comparable<ResourceRequestInterceptor>, ResourceInterceptor {

    private BeanFactory<ContainerRequestFilter> factory;
    private boolean preMatching;
    private Integer priority = Priorities.USER; // default priority as defined by spec

    /**
     * The class names of the {@code @NameBinding} annotations that the method is annotated with.
     */
    private Set<String> nameBindingNames = Collections.emptySet();

    public void setFactory(BeanFactory<ContainerRequestFilter> factory) {
        this.factory = factory;
    }

    public BeanFactory<ContainerRequestFilter> getFactory() {
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
    public int compareTo(ResourceRequestInterceptor o) {
        return this.priority.compareTo(o.priority);
    }

    //TODO: move somewhere else?
    public static class ClosingTask implements Closeable {
        private final Collection<BeanFactory.BeanInstance<ContainerRequestFilter>> instances;

        public ClosingTask(Collection<BeanFactory.BeanInstance<ContainerRequestFilter>> instances) {
            this.instances = instances;
        }

        @Override
        public void close() throws IOException {
            for (BeanFactory.BeanInstance<ContainerRequestFilter> i : instances) {
                i.close();
            }
        }
    }
}
