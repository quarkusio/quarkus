package io.quarkus.rest.runtime.model;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerResponseFilter;

import io.quarkus.rest.runtime.spi.BeanFactory;

public class ResourceResponseInterceptor implements Comparable<ResourceResponseInterceptor>, ResourceInterceptor {

    private BeanFactory<ContainerResponseFilter> factory;
    private Integer priority = Priorities.USER; // default priority as defined by spec

    /**
     * The class names of the {@code @NameBinding} annotations that the method is annotated with.
     */
    private Set<String> nameBindingNames = Collections.emptySet();

    public void setFactory(BeanFactory<ContainerResponseFilter> factory) {
        this.factory = factory;
    }

    public BeanFactory<ContainerResponseFilter> getFactory() {
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

    // spec says that response interceptors are sorted in descending order
    @Override
    public int compareTo(ResourceResponseInterceptor o) {
        return o.priority.compareTo(this.priority);
    }

    //TODO: move somewhere else?
    public static class ClosingTask implements Closeable {
        private final Collection<BeanFactory.BeanInstance<ContainerResponseFilter>> instances;

        public ClosingTask(Collection<BeanFactory.BeanInstance<ContainerResponseFilter>> instances) {
            this.instances = instances;
        }

        @Override
        public void close() throws IOException {
            for (BeanFactory.BeanInstance<ContainerResponseFilter> i : instances) {
                i.close();
            }
        }
    }
}
