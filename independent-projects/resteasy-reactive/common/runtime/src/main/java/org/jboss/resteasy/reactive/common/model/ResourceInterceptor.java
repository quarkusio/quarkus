package org.jboss.resteasy.reactive.common.model;

import jakarta.ws.rs.Priorities;
import java.util.Collections;
import java.util.Set;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class ResourceInterceptor<T>
        implements Comparable<ResourceInterceptor<T>>, SettableResourceInterceptor<T>, HasPriority {

    private BeanFactory<T> factory;
    private int priority = Priorities.USER; // default priority as defined by spec
    private boolean nonBlockingRequired; // whether or not @NonBlocking was specified on the class
    private boolean readBody; // whether or not 'readBody' was set true for this filter

    /**
     * The class names of the {@code @NameBinding} annotations that the method is annotated with.
     */
    private Set<String> nameBindingNames = Collections.emptySet();

    private String className;

    public void setFactory(BeanFactory<T> factory) {
        this.factory = factory;
    }

    public BeanFactory<T> getFactory() {
        return factory;
    }

    public Integer getPriority() {
        return priority;
    }

    public Integer priority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        if (priority == null) {
            this.priority = Priorities.USER;
        } else {
            this.priority = priority;
        }
    }

    public Set<String> getNameBindingNames() {
        return nameBindingNames;
    }

    public void setNameBindingNames(Set<String> nameBindingNames) {
        this.nameBindingNames = nameBindingNames;
    }

    public String getClassName() {
        return className;
    }

    public ResourceInterceptor<T> setClassName(String className) {
        this.className = className;
        return this;
    }

    public boolean isNonBlockingRequired() {
        return nonBlockingRequired;
    }

    public void setNonBlockingRequired(boolean nonBlockingRequired) {
        this.nonBlockingRequired = nonBlockingRequired;
    }

    public boolean isReadBody() {
        return readBody;
    }

    public void setReadBody(boolean readBody) {
        this.readBody = readBody;
    }

    // spec says that writer interceptors are sorted in ascending order
    @Override
    public int compareTo(ResourceInterceptor<T> o) {
        return this.priority().compareTo(o.priority());
    }

    //Container response filters are reversed
    public static class Reversed<T> extends ResourceInterceptor<T> {

        @Override
        public Integer priority() {
            Integer p = super.priority();
            if (p == null) {
                return null;
            }
            return -p;
        }
    }
}
