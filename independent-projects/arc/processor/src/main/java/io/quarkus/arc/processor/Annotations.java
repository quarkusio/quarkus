package io.quarkus.arc.processor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

public final class Annotations {

    private Annotations() {
    }

    static AnnotationInstance find(Collection<AnnotationInstance> annotations, DotName name) {
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

    static boolean contains(Collection<AnnotationInstance> annotations, DotName name) {
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

    static boolean containsAny(Collection<AnnotationInstance> annotations, Iterable<DotName> names) {
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

    static Set<AnnotationInstance> getParameterAnnotations(BeanDeployment beanDeployment, MethodInfo method, int position) {
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
