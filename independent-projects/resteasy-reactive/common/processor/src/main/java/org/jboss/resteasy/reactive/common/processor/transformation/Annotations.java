package org.jboss.resteasy.reactive.common.processor.transformation;

import java.util.Collection;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

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

}
