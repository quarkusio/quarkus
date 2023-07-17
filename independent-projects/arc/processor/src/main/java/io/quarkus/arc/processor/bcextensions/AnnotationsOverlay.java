package io.quarkus.arc.processor.bcextensions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.jboss.jandex.DotName;

// JandexDeclaration must be a Jandex declaration for which Arc supports annotation transformations
// directly (classes, methods, fields) or indirectly (parameters); see also AnnotationsTransformation
abstract class AnnotationsOverlay<JandexDeclaration extends org.jboss.jandex.AnnotationTarget> {
    private final Map<org.jboss.jandex.EquivalenceKey, AnnotationSet> overlay = new ConcurrentHashMap<>();
    private volatile boolean invalid = false;

    AnnotationSet getAnnotations(JandexDeclaration jandexDeclaration, org.jboss.jandex.IndexView jandexIndex) {
        if (invalid) {
            throw new IllegalStateException("Annotations overlay no longer valid");
        }

        org.jboss.jandex.EquivalenceKey key = org.jboss.jandex.EquivalenceKey.of(jandexDeclaration);
        if (overlay.containsKey(key)) {
            return overlay.get(key);
        }

        AnnotationSet annotationSet = createAnnotationSet(jandexDeclaration, jandexIndex);
        overlay.put(key, annotationSet);
        return annotationSet;
    }

    boolean hasAnnotation(JandexDeclaration jandexDeclaration, DotName annotationName, org.jboss.jandex.IndexView jandexIndex) {
        if (invalid) {
            throw new IllegalStateException("Annotations overlay no longer valid");
        }

        org.jboss.jandex.EquivalenceKey key = org.jboss.jandex.EquivalenceKey.of(jandexDeclaration);
        boolean hasOverlay = overlay.containsKey(key);

        if (hasOverlay) {
            return getAnnotations(jandexDeclaration, jandexIndex).hasAnnotation(annotationName);
        } else {
            return originalJandexAnnotationsContain(jandexDeclaration, annotationName, jandexIndex);
        }
    }

    void invalidate() {
        overlay.clear();
        invalid = true;
    }

    abstract AnnotationSet createAnnotationSet(JandexDeclaration jandexDeclaration, org.jboss.jandex.IndexView jandexIndex);

    // this is "just" an optimization to avoid creating and populating an `AnnotationSet`
    // when the only thing we need to know is if an annotation is present
    abstract boolean originalJandexAnnotationsContain(JandexDeclaration jandexDeclaration, DotName annotationName,
            org.jboss.jandex.IndexView jandexIndex);

    static class Classes extends AnnotationsOverlay<org.jboss.jandex.ClassInfo> {
        @Override
        AnnotationSet createAnnotationSet(org.jboss.jandex.ClassInfo classInfo,
                org.jboss.jandex.IndexView jandexIndex) {
            // if an `@Inherited` annotation of some type is declared directly on class C, then annotations
            // of the same type declared directly on any direct or indirect superclass are _not_ present on C
            Set<DotName> alreadySeen = new HashSet<>();

            List<org.jboss.jandex.AnnotationInstance> jandexAnnotations = new ArrayList<>();
            Map<DotName, Integer> inheritanceDistances = new ConcurrentHashMap<>();

            int currentDistance = 0;
            while (classInfo != null && !classInfo.name().equals(DotNames.OBJECT)) {
                for (org.jboss.jandex.AnnotationInstance jandexAnnotation : classInfo.declaredAnnotations()) {
                    if (!jandexAnnotation.runtimeVisible()) {
                        continue;
                    }

                    if (alreadySeen.contains(jandexAnnotation.name())) {
                        continue;
                    }
                    alreadySeen.add(jandexAnnotation.name());

                    jandexAnnotations.add(jandexAnnotation);
                    inheritanceDistances.put(jandexAnnotation.name(), currentDistance);
                }

                DotName superClassName = classInfo.superName();
                classInfo = jandexIndex.getClassByName(superClassName);
                currentDistance++;
            }

            return new AnnotationSet(jandexAnnotations, inheritanceDistances);
        }

        @Override
        boolean originalJandexAnnotationsContain(org.jboss.jandex.ClassInfo classInfo, DotName annotationName,
                org.jboss.jandex.IndexView jandexIndex) {
            while (classInfo != null && !classInfo.name().equals(DotNames.OBJECT)) {
                org.jboss.jandex.AnnotationInstance jandexAnnotation = classInfo.declaredAnnotation(annotationName);
                if (jandexAnnotation != null && jandexAnnotation.runtimeVisible()) {
                    return true;
                }

                DotName superClassName = classInfo.superName();
                classInfo = jandexIndex.getClassByName(superClassName);
            }
            return false;
        }
    }

    static class Methods extends AnnotationsOverlay<org.jboss.jandex.MethodInfo> {
        @Override
        AnnotationSet createAnnotationSet(org.jboss.jandex.MethodInfo methodInfo,
                org.jboss.jandex.IndexView jandexIndex) {
            List<org.jboss.jandex.AnnotationInstance> jandexAnnotations = methodInfo.declaredAnnotations()
                    .stream()
                    .filter(org.jboss.jandex.AnnotationInstance::runtimeVisible)
                    .collect(Collectors.toUnmodifiableList());
            return new AnnotationSet(jandexAnnotations);
        }

        @Override
        boolean originalJandexAnnotationsContain(org.jboss.jandex.MethodInfo methodInfo, DotName annotationName,
                org.jboss.jandex.IndexView jandexIndex) {
            org.jboss.jandex.AnnotationInstance jandexAnnotation = methodInfo.declaredAnnotation(annotationName);
            return jandexAnnotation != null && jandexAnnotation.runtimeVisible();
        }
    }

    static class Parameters extends AnnotationsOverlay<org.jboss.jandex.MethodParameterInfo> {
        @Override
        AnnotationSet createAnnotationSet(org.jboss.jandex.MethodParameterInfo methodParameterInfo,
                org.jboss.jandex.IndexView jandexIndex) {
            List<org.jboss.jandex.AnnotationInstance> jandexAnnotations = methodParameterInfo.declaredAnnotations()
                    .stream()
                    .filter(org.jboss.jandex.AnnotationInstance::runtimeVisible)
                    .collect(Collectors.toUnmodifiableList());
            return new AnnotationSet(jandexAnnotations);
        }

        @Override
        boolean originalJandexAnnotationsContain(org.jboss.jandex.MethodParameterInfo methodParameterInfo,
                DotName annotationName, org.jboss.jandex.IndexView jandexIndex) {
            org.jboss.jandex.AnnotationInstance jandexAnnotation = methodParameterInfo.declaredAnnotation(annotationName);
            return jandexAnnotation != null && jandexAnnotation.runtimeVisible();
        }
    }

    static class Fields extends AnnotationsOverlay<org.jboss.jandex.FieldInfo> {
        @Override
        AnnotationSet createAnnotationSet(org.jboss.jandex.FieldInfo fieldInfo,
                org.jboss.jandex.IndexView jandexIndex) {
            List<org.jboss.jandex.AnnotationInstance> jandexAnnotations = fieldInfo.declaredAnnotations()
                    .stream()
                    .filter(org.jboss.jandex.AnnotationInstance::runtimeVisible)
                    .collect(Collectors.toUnmodifiableList());
            return new AnnotationSet(jandexAnnotations);
        }

        @Override
        boolean originalJandexAnnotationsContain(org.jboss.jandex.FieldInfo fieldInfo, DotName annotationName,
                org.jboss.jandex.IndexView jandexIndex) {
            org.jboss.jandex.AnnotationInstance jandexAnnotation = fieldInfo.declaredAnnotation(annotationName);
            return jandexAnnotation != null && jandexAnnotation.runtimeVisible();
        }
    }
}
