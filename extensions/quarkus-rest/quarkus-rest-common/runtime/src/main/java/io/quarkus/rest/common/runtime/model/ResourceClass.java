package io.quarkus.rest.common.runtime.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.quarkus.rest.common.runtime.util.URLUtils;
import io.quarkus.rest.spi.BeanFactory;

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

    private BeanFactory<Object> factory;
    private boolean perRequestResource;

    private boolean isFormParamRequired;

    private Set<String> pathParameters = new HashSet<>();

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
        if (path != null) {
            pathParameters.clear();
            URLUtils.parsePathParameters(path, pathParameters);
        }
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

    public boolean isFormParamRequired() {
        return isFormParamRequired;
    }

    public ResourceClass setFormParamRequired(boolean isFormParamRequired) {
        this.isFormParamRequired = isFormParamRequired;
        return this;
    }

    public Set<String> getPathParameters() {
        return pathParameters;
    }

    public ResourceClass setPathParameters(Set<String> pathParameters) {
        this.pathParameters = pathParameters;
        return this;
    }
}
