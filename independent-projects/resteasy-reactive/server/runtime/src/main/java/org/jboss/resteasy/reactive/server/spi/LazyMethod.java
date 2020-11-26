package org.jboss.resteasy.reactive.server.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import javax.ws.rs.container.ResourceInfo;

/**
 * A lazy representation of a Method
 *
 * Only loaded if actually needed, which should not be the case generally
 * unless custom Serialization is in use.
 */
public class LazyMethod implements ResourceInfo {

    private final String name;
    private final Class<?> declaringClass;
    private final Class[] parameterTypes;
    private volatile Method method;
    private volatile Annotation[] annotations;
    private volatile Type returnType;

    public LazyMethod(String name, Class<?> declaringClass, Class[] parameterTypes) {
        this.name = name;
        this.declaringClass = declaringClass;
        this.parameterTypes = parameterTypes;
    }

    public String getName() {
        return name;
    }

    public Class[] getParameterTypes() {
        return parameterTypes;
    }

    public Method getMethod() {
        if (method == null) {
            synchronized (this) {
                if (method == null) {
                    try {
                        Method declaredMethod = declaringClass.getMethod(name, parameterTypes);
                        annotations = declaredMethod.getAnnotations();
                        returnType = declaredMethod.getGenericReturnType();
                        method = declaredMethod;
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return method;
    }

    public Annotation[] getAnnotations() {
        if (annotations == null) {
            getMethod();
        }
        return annotations;
    }

    public Type getGenericReturnType() {
        if (returnType == null) {
            getMethod();
        }
        return returnType;
    }

    @Override
    public Method getResourceMethod() {
        return getMethod();
    }

    @Override
    public Class<?> getResourceClass() {
        return declaringClass;
    }

    public Annotation[] getParameterAnnotations(int index) {
        // Should we cache this?
        return getMethod().getParameterAnnotations()[index];
    }
}
