package io.quarkus.rest.common.runtime.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.ws.rs.ext.ExceptionMapper;

import io.quarkus.rest.common.runtime.core.LazyUnmanagedBeanFactory;
import io.quarkus.rest.common.runtime.util.URLUtils;
import io.quarkus.rest.spi.BeanFactory;
import io.quarkus.runtime.annotations.IgnoreProperty;

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

    /**
     * Contains class level exception mappers
     * The key is the exception type and the value is the exception mapper class
     */
    private Map<String, String> classLevelExceptionMappers = new HashMap<>();

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

    public Map<String, String> getClassLevelExceptionMappers() {
        return classLevelExceptionMappers;
    }

    public void setClassLevelExceptionMappers(Map<String, String> classLevelExceptionMappers) {
        this.classLevelExceptionMappers = classLevelExceptionMappers;
    }

    @IgnoreProperty
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Map<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> getResourceExceptionMapper() {
        if (classLevelExceptionMappers.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> result = new HashMap<>(
                classLevelExceptionMappers.size());
        for (Map.Entry<String, String> entry : classLevelExceptionMappers.entrySet()) {
            ResourceExceptionMapper mapper = new ResourceExceptionMapper();
            // TODO: consider not using reflection to create these
            mapper.setFactory(new LazyUnmanagedBeanFactory<>(new Supplier<ExceptionMapper<?>>() {
                @Override
                public ExceptionMapper<?> get() {
                    try {
                        return (ExceptionMapper<?>) loadClass(entry.getValue()).getConstructor().newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }));
            result.put((Class<? extends Throwable>) loadClass(entry.getKey()), mapper);
        }
        return result;
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
