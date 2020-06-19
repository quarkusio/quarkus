package io.quarkus.qrs.runtime.model;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.qrs.runtime.spi.EndpointFactory;

public class ResourceClass {

    /**
     * The class name of the resource class
     */
    private String className;

    /**
     * The class path, will be null if this is a sub resource
     */
    private String path;

    /**
     * The resource methods
     */
    private final List<ResourceMethod> methods = new ArrayList<>();

    private EndpointFactory factory;

    public boolean isSubResource() {
        return path == null;
    }

    public String getClassName() {
        return className;
    }

    public ResourceClass setClassName(String className) {
        this.className = className;
        return this;
    }

    public String getPath() {
        return path;
    }

    public EndpointFactory getFactory() {
        return factory;
    }

    public ResourceClass setFactory(EndpointFactory factory) {
        this.factory = factory;
        return this;
    }

    public ResourceClass setPath(String path) {
        this.path = path;
        return this;
    }

    public List<ResourceMethod> getMethods() {
        return methods;
    }
}
