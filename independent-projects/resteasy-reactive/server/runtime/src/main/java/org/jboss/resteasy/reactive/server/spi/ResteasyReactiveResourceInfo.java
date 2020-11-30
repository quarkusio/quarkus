package org.jboss.resteasy.reactive.server.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Set;
import javax.ws.rs.container.ResourceInfo;

/**
 * A lazy representation of a Method
 *
 * Only loaded if actually needed, which should not be the case generally
 * unless custom Serialization is in use.
 */
public class ResteasyReactiveResourceInfo implements ResourceInfo {

    private final String name;
    private final Class<?> declaringClass;
    private final Class[] parameterTypes;
    private final Set<String> methodAnnotationNames;
    private volatile Method method;
    private volatile Annotation[] annotations;
    private volatile Type returnType;

    public ResteasyReactiveResourceInfo(String name, Class<?> declaringClass, Class[] parameterTypes,
            Set<String> methodAnnotationNames) {
        this.name = name;
        this.declaringClass = declaringClass;
        this.parameterTypes = parameterTypes;
        this.methodAnnotationNames = methodAnnotationNames;
    }

    public String getName() {
        return name;
    }

    public Class[] getParameterTypes() {
        return parameterTypes;
    }

    public Set<String> getMethodAnnotationNames() {
        return methodAnnotationNames;
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
