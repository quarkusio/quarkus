package org.jboss.resteasy.reactive.client.impl;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.InterceptorContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractClientInterceptorContextImpl implements InterceptorContext {

    protected MediaType mediaType;
    protected Type entityType;
    protected Class<?> entityClass;
    protected Annotation[] annotations;
    protected Map<String, Object> properties;

    public AbstractClientInterceptorContextImpl(Annotation[] annotations, Class<?> entityClass, Type entityType,
            MediaType mediaType, Map<String, Object> properties) {
        this.annotations = annotations;
        this.entityClass = entityClass;
        this.entityType = entityType;
        this.mediaType = mediaType;
        this.properties = properties;
    }

    @Override
    public Object getProperty(String name) {
        return properties.get(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        // TCK says the property names need to be immutable
        return Collections.unmodifiableSet(properties.keySet());
    }

    @Override
    public void setProperty(String name, Object object) {
        properties.put(name, object);
    }

    @Override
    public void removeProperty(String name) {
        properties.remove(name);
    }

    @Override
    public Annotation[] getAnnotations() {
        return annotations;
    }

    @Override
    public void setAnnotations(Annotation[] annotations) {
        Objects.requireNonNull(annotations);
        this.annotations = annotations;
    }

    @Override
    public Class<?> getType() {
        return entityClass;
    }

    @Override
    public void setType(Class<?> type) {
        entityClass = type;
        // FIXME: invalidate generic type?
    }

    @Override
    public Type getGenericType() {
        return entityType;
    }

    @Override
    public void setGenericType(Type genericType) {
        // FIXME: invalidate entity class?
        entityType = genericType;
    }

    @Override
    public MediaType getMediaType() {
        return mediaType;
    }

    @Override
    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }
}
