package org.jboss.resteasy.reactive.common.processor.transformation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

final class Annotations {

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
        return find(annotations, name) != null;
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
        return getAnnotations(Kind.METHOD_PARAMETER, annotations);
    }

    /**
     * 
     * @param annotations
     * @return the annotations for the given kind
     */
    public static Set<AnnotationInstance> getAnnotations(Kind kind, Collection<AnnotationInstance> annotations) {
        return getAnnotations(kind, null, annotations);
    }

    /**
     * 
     * @param annotations
     * @return the annotations for the given kind and name
     */
    public static Set<AnnotationInstance> getAnnotations(Kind kind, DotName name, Collection<AnnotationInstance> annotations) {
        if (annotations.isEmpty()) {
            return Collections.emptySet();
        }
        Set<AnnotationInstance> ret = new HashSet<>();
        for (AnnotationInstance annotation : annotations) {
            if (kind != annotation.target().kind()) {
                continue;
            }
            if (name != null && !annotation.name().equals(name)) {
                continue;
            }
            ret.add(annotation);
        }
        return ret;
    }

    /**
     * 
     * @param transformedAnnotations
     * @param method
     * @param position
     * @return the parameter annotations for the given position
     */
    public static Set<AnnotationInstance> getParameterAnnotations(
            Function<AnnotationTarget, Collection<AnnotationInstance>> transformedAnnotations, MethodInfo method,
            int position) {
        Set<AnnotationInstance> annotations = new HashSet<>();
        for (AnnotationInstance annotation : transformedAnnotations.apply(method)) {
            if (Kind.METHOD_PARAMETER == annotation.target().kind()
                    && annotation.target().asMethodParameter().position() == position) {
                annotations.add(annotation);
            }
        }
        return annotations;
    }

}
