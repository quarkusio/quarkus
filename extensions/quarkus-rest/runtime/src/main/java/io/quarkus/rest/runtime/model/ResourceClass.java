package io.quarkus.rest.runtime.model;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.rest.runtime.spi.BeanFactory;

public class ResourceClass implements FormContainer {

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

    private BeanFactory<Object> factory;
    private boolean perRequestResource;

    private boolean isFormParamRequired;

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

    public BeanFactory<Object> getFactory() {
        return factory;
    }

    public ResourceClass setFactory(BeanFactory<Object> factory) {
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

    public boolean isPerRequestResource() {
        return perRequestResource;
    }

    public void setPerRequestResource(boolean perRequestResource) {
        this.perRequestResource = perRequestResource;
    }

    @Override
    public boolean isFormParamRequired() {
        return isFormParamRequired;
    }

    @Override
    public ResourceClass setFormParamRequired(boolean isFormParamRequired) {
        this.isFormParamRequired = isFormParamRequired;
        return this;
    }
}
