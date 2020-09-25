package io.quarkus.rest.runtime.jaxrs;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.InterceptorContext;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;

public abstract class QuarkusRestAbstractInterceptorContext implements InterceptorContext {
    protected final QuarkusRestRequestContext context;
    protected Annotation[] annotations;
    protected Class<?> type;
    protected Type genericType;
    protected MediaType mediaType;

    public QuarkusRestAbstractInterceptorContext(QuarkusRestRequestContext context, Annotation[] annotations, Class<?> type,
            Type genericType, MediaType mediaType) {
        this.context = context;
        this.annotations = annotations;
        this.type = type;
        this.genericType = genericType;
        this.mediaType = mediaType;
    }

    public Object getProperty(String name) {
        return context.getProperty(name);
    }

    public Collection<String> getPropertyNames() {
        return context.getPropertyNames();
    }

    public void setProperty(String name, Object object) {
        context.setProperty(name, object);
    }

    public void removeProperty(String name) {
        context.removeProperty(name);
    }

    public Annotation[] getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Annotation[] annotations) {
        if (annotations == null) {
            throw new NullPointerException("parameters cannot be null");
        }
        this.annotations = annotations;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public Type getGenericType() {
        return genericType;
    }

    public void setGenericType(Type genericType) {
        this.genericType = genericType;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

}
