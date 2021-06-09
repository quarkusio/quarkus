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

    private static final String JSON_VIEW_NAME = "com.fasterxml.jackson.annotation.JsonView";
    private static final String CUSTOM_SERIALIZATION = "io.quarkus.resteasy.reactive.jackson.CustomSerialization";

    private final String name;
    private final Class<?> declaringClass;
    private final Class[] parameterTypes;
    private final Set<String> classAnnotationNames;
    private final Set<String> methodAnnotationNames;
    private final boolean requiresCustomSerialization;
    private final boolean requiresJsonViewName;

    private volatile Annotation[] classAnnotations;
    private volatile Method method;
    private volatile Annotation[] annotations;
    private volatile Type returnType;

    public ResteasyReactiveResourceInfo(String name, Class<?> declaringClass, Class[] parameterTypes,
            Set<String> classAnnotationNames, Set<String> methodAnnotationNames) {
        this.name = name;
        this.declaringClass = declaringClass;
        this.parameterTypes = parameterTypes;
        this.classAnnotationNames = classAnnotationNames;
        this.methodAnnotationNames = methodAnnotationNames;
        this.requiresCustomSerialization = methodAnnotationNames.contains(CUSTOM_SERIALIZATION);
        this.requiresJsonViewName = methodAnnotationNames.contains(JSON_VIEW_NAME);
    }

    public String getName() {
        return name;
    }

    public Class[] getParameterTypes() {
        return parameterTypes;
    }

    public Set<String> getClassAnnotationNames() {
        return classAnnotationNames;
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

    public Annotation[] getClassAnnotations() {
        if (classAnnotations == null) {
            classAnnotations = declaringClass.getAnnotations();
        }
        return classAnnotations;
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

    /**
     * Short cut to check if the method is annotated with io.quarkus.resteasy.reactive.jackson.CustomSerialization
     */
    public boolean requiresCustomSerialization() {
        return this.requiresCustomSerialization;
    }

    /**
     * Short cut to check if the method is annotated with com.fasterxml.jackson.annotation.JsonView
     */
    public boolean requiresJsonViewName() {
        return this.requiresJsonViewName;
    }
}
