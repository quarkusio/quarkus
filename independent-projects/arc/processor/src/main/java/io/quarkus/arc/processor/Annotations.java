package io.quarkus.arc.processor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

public final class Annotations {

    private Annotations() {
    }

    /**
     * 
     * @param annotations
     * @param name
     * @return the first matching annotation instance with the given name or null
     */
    public static AnnotationInstance find(Collection<AnnotationInstance> annotations, DotName name) {
        if (annotations.isEmpty()) {
            return null;
        }
        for (AnnotationInstance annotationInstance : annotations) {
            if (annotationInstance.name().equals(name)) {
                return annotationInstance;
            }
        }
        return null;
    }

    /**
     * 
     * @param annotations
     * @param name
     * @return {@code true} if the given collection contains an annotation instance with the given name, {@code false} otherwise
     */
    public static boolean contains(Collection<AnnotationInstance> annotations, DotName name) {
        if (annotations.isEmpty()) {
            return false;
        }
        for (AnnotationInstance annotationInstance : annotations) {
            if (annotationInstance.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * @param annotations
     * @param names
     * @return {@code true} if the given collection contains an annotation instance with any of the given names, {@code false}
     *         otherwise
     */
    public static boolean containsAny(Collection<AnnotationInstance> annotations, Iterable<DotName> names) {
        if (annotations.isEmpty()) {
            return false;
        }
        for (AnnotationInstance annotationInstance : annotations) {
            for (DotName name : names) {
                if (annotationInstance.name().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 
     * @param annotations
     * @return the parameter annotations
     */
    public static Set<AnnotationInstance> getParameterAnnotations(Collection<AnnotationInstance> annotations) {
        if (annotations.isEmpty()) {
            return Collections.emptySet();
        }
        Set<AnnotationInstance> ret = new HashSet<>();
        for (AnnotationInstance annotation : annotations) {
            if (Kind.METHOD_PARAMETER == annotation.target().kind()) {
                ret.add(annotation);
            }
        }
        return ret;
    }

    /**
     * 
     * @param beanDeployment
     * @param method
     * @param position
     * @return the parameter annotations for the given position
     */
    public static Set<AnnotationInstance> getParameterAnnotations(BeanDeployment beanDeployment, MethodInfo method,
            int position) {
        Set<AnnotationInstance> annotations = new HashSet<>();
        for (AnnotationInstance annotation : beanDeployment.getAnnotations(method)) {
            if (Kind.METHOD_PARAMETER == annotation.target().kind()
                    && annotation.target().asMethodParameter().position() == position) {
                annotations.add(annotation);
            }
        }
        return annotations;
    }

    /**
     * Iterates over all annotations on a method and its parameters, filters out all non-parameter annotations
     * and returns a first encountered {@link AnnotationInstance} with Annotation specified as {@link DotName}.
     * Returns {@code null} if no such annotation exists.
     *
     * @param method MethodInfo to be searched for annotations
     * @param annotation Annotation we are looking for, represented as DotName
     * @return First encountered {@link AnnotationInstance} fitting the requirements, {@code null} if none is found
     */
    public static AnnotationInstance getParameterAnnotation(MethodInfo method, DotName annotation) {
        for (AnnotationInstance annotationInstance : method.annotations()) {
            if (annotationInstance.target().kind().equals(Kind.METHOD_PARAMETER) &&
                    annotationInstance.name().equals(annotation)) {
                return annotationInstance;
            }
        }
        return null;
    }

}
