package org.jboss.resteasy.reactive.server.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import jakarta.ws.rs.container.ResourceInfo;

import org.jboss.resteasy.reactive.server.util.MethodId;

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
    /**
     * If it's non-blocking method within the runtime that won't always default to blocking
     */
    public final boolean isNonBlocking;
    /**
     * This class name will only differ from {@link this#declaringClass} name when the {@link this#method} was inherited.
     */
    private final String actualDeclaringClassName;
    private volatile Method method;
    private volatile Annotation[] annotations;
    private volatile Type returnType;
    private volatile String methodId;

    public ResteasyReactiveResourceInfo(String name, Class<?> declaringClass, Class[] parameterTypes,
            boolean isNonBlocking,
            String actualDeclaringClassName) {
        this.name = name;
        this.declaringClass = declaringClass;
        this.parameterTypes = parameterTypes;
        this.isNonBlocking = isNonBlocking;
        this.actualDeclaringClassName = actualDeclaringClassName;
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

    public String getMethodId() {
        if (methodId == null) {
            methodId = MethodId.get(name, declaringClass, parameterTypes);
        }
        return methodId;
    }

    /**
     * @return declaring class of a method that returns endpoint response
     * @deprecated if you need the method, please open an issue so that we can document and test your use case
     */
    @Deprecated
    public String getActualDeclaringClassName() {
        return actualDeclaringClassName;
    }
}
