package io.quarkus.arc.processor.bcextensions;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.jboss.jandex.DotName;

class AnnotationSet {
    private final Map<DotName, org.jboss.jandex.AnnotationInstance> data;

    // used only when this AnnotationSet represents annotations on a class declaration,
    // because in such case, annotations may be inherited from superclasses
    //
    // for each annotation type, this map contains a distance from the original class
    // to the class on which an annotation of that type is declared (0 means the annotation
    // is declared directly on the original class, 1 means the annotation is declared
    // directly on a direct superclass of the original class, etc.)
    //
    // if this AnnotationSet represents annotations on any other annotation target,
    // this map contains 0 for all annotation types
    private final Map<DotName, Integer> inheritanceDistances;

    private static Map<DotName, Integer> zeroDistances(Collection<org.jboss.jandex.AnnotationInstance> jandexAnnotations) {
        Map<DotName, Integer> distances = new ConcurrentHashMap<>();
        for (org.jboss.jandex.AnnotationInstance jandexAnnotation : jandexAnnotations) {
            distances.put(jandexAnnotation.name(), 0);
        }
        return distances;
    }

    AnnotationSet(Collection<org.jboss.jandex.AnnotationInstance> jandexAnnotations) {
        this(jandexAnnotations, zeroDistances(jandexAnnotations));
    }

    AnnotationSet(Collection<org.jboss.jandex.AnnotationInstance> jandexAnnotations,
            Map<DotName, Integer> inheritanceDistances) {
        Map<DotName, org.jboss.jandex.AnnotationInstance> data = new ConcurrentHashMap<>();
        for (org.jboss.jandex.AnnotationInstance jandexAnnotation : jandexAnnotations) {
            data.put(jandexAnnotation.name(), jandexAnnotation);
        }
        this.data = data;
        this.inheritanceDistances = inheritanceDistances;
    }

    boolean hasAnnotation(Class<? extends Annotation> annotationType) {
        DotName name = DotName.createSimple(annotationType.getName());
        return hasAnnotation(name);
    }

    boolean hasAnnotation(DotName annotationName) {
        return data.containsKey(annotationName);
    }

    org.jboss.jandex.AnnotationInstance annotation(Class<? extends Annotation> annotationType) {
        DotName name = DotName.createSimple(annotationType.getName());
        return data.get(name);
    }

    Collection<org.jboss.jandex.AnnotationInstance> annotationsWithRepeatable(Class<? extends Annotation> annotationType) {
        Repeatable repeatable = annotationType.getAnnotation(Repeatable.class);

        DotName name = DotName.createSimple(annotationType.getName());
        DotName containerName = repeatable != null
                ? DotName.createSimple(repeatable.value().getName())
                : DotName.OBJECT_NAME; // not an annotation name, so never present in the map

        if (data.containsKey(name) && data.containsKey(containerName)) {
            int annDistance = inheritanceDistances.get(name);
            int containerAnnDistance = inheritanceDistances.get(containerName);
            if (annDistance < containerAnnDistance) {
                return List.of(data.get(name));
            } else if (annDistance == containerAnnDistance) {
                // equal inheritance distances may happen if a single annotation of a repeatable annotation type
                // is declared, and an annotation of the containing annotation type is also (explicitly!) declared
                // (on the same annotation target)
                List<org.jboss.jandex.AnnotationInstance> result = new ArrayList<>();
                result.add(data.get(name));
                org.jboss.jandex.AnnotationInstance container = data.get(containerName);
                org.jboss.jandex.AnnotationInstance[] values = container.value().asNestedArray();
                result.addAll(Arrays.asList(values));
                return result;
            } else {
                org.jboss.jandex.AnnotationInstance container = data.get(containerName);
                org.jboss.jandex.AnnotationInstance[] values = container.value().asNestedArray();
                return List.of(values);
            }
        } else if (data.containsKey(name)) {
            return List.of(data.get(name));
        } else if (data.containsKey(containerName)) {
            org.jboss.jandex.AnnotationInstance container = data.get(containerName);
            org.jboss.jandex.AnnotationInstance[] values = container.value().asNestedArray();
            return List.of(values);
        } else {
            return List.of();
        }
    }

    Collection<org.jboss.jandex.AnnotationInstance> annotations() {
        return Collections.unmodifiableCollection(data.values());
    }

    // ---
    // modifications, can only be called from AnnotationsTransformation

    void add(org.jboss.jandex.AnnotationInstance jandexAnnotation) {
        data.put(jandexAnnotation.name(), jandexAnnotation);
        inheritanceDistances.put(jandexAnnotation.name(), 0);
    }

    void removeIf(Predicate<org.jboss.jandex.AnnotationInstance> predicate) {
        Set<DotName> toRemove = new HashSet<>();
        for (org.jboss.jandex.AnnotationInstance jandexAnnotation : data.values()) {
            if (predicate.test(jandexAnnotation)) {
                toRemove.add(jandexAnnotation.name());
            }
        }

        for (DotName name : toRemove) {
            data.remove(name);
            inheritanceDistances.remove(name);
        }
    }
}
