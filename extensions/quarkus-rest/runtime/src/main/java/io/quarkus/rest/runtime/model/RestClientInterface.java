package io.quarkus.rest.runtime.model;

import java.util.ArrayList;
import java.util.List;

public class RestClientInterface {

    /**
     * The class name of the interface
     */
    private String className;

    /**
     * The class path
     */
    private String path;

    /**
     * The resource methods
     */
    private final List<ResourceMethod> methods = new ArrayList<>();

    public String getClassName() {
        return className;
    }

    public RestClientInterface setClassName(String className) {
        this.className = className;
        return this;
    }

    public String getPath() {
        return path;
    }

    public RestClientInterface setPath(String path) {
        this.path = path;
        return this;
    }

    public List<ResourceMethod> getMethods() {
        return methods;
    }
}
