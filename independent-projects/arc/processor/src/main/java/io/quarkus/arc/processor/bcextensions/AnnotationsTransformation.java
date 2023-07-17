package io.quarkus.arc.processor.bcextensions;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import jakarta.enterprise.lang.model.AnnotationInfo;

import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.Annotations;

// this must be symmetric with AnnotationsOverlay
abstract class AnnotationsTransformation<JandexDeclaration extends org.jboss.jandex.AnnotationTarget>
        implements io.quarkus.arc.processor.AnnotationsTransformer {

    final org.jboss.jandex.IndexView jandexIndex;
    final AllAnnotationOverlays annotationOverlays;

    private final org.jboss.jandex.AnnotationTarget.Kind kind;
    private final Map<org.jboss.jandex.EquivalenceKey, List<Consumer<TransformationContext>>> transformations = new ConcurrentHashMap<>();

    private volatile boolean frozen = false;

    AnnotationsTransformation(org.jboss.jandex.IndexView jandexIndex, AllAnnotationOverlays annotationOverlays,
            org.jboss.jandex.AnnotationTarget.Kind kind) {
        this.jandexIndex = jandexIndex;
        this.annotationOverlays = annotationOverlays;
        this.kind = kind;
    }

    private void addAnnotation(JandexDeclaration jandexDeclaration, org.jboss.jandex.AnnotationInstance jandexAnnotation) {
        if (frozen) {
            throw new IllegalStateException("Annotations transformation frozen");
        }

        org.jboss.jandex.EquivalenceKey key = org.jboss.jandex.EquivalenceKey.of(jandexDeclaration);

        org.jboss.jandex.AnnotationInstance jandexAnnotationWithTarget = org.jboss.jandex.AnnotationInstance.create(
                jandexAnnotation.name(), jandexDeclaration, jandexAnnotation.values());

        annotationsOverlay().getAnnotations(jandexDeclaration, jandexIndex).add(jandexAnnotationWithTarget);

        Consumer<TransformationContext> transformation = ctx -> {
            ctx.transform().add(jandexAnnotationWithTarget).done();
        };
        transformations.computeIfAbsent(key, ignored -> new ArrayList<>()).add(transformation);
    }

    void addAnnotation(JandexDeclaration jandexDeclaration, Class<? extends Annotation> clazz) {
        org.jboss.jandex.AnnotationInstance jandexAnnotation = org.jboss.jandex.AnnotationInstance.create(
                DotName.createSimple(clazz.getName()), null, AnnotationValueArray.EMPTY);

        addAnnotation(jandexDeclaration, jandexAnnotation);
    }

    void addAnnotation(JandexDeclaration jandexDeclaration, AnnotationInfo annotation) {
        addAnnotation(jandexDeclaration, ((AnnotationInfoImpl) annotation).jandexAnnotation);
    }

    void addAnnotation(JandexDeclaration jandexDeclaration, Annotation annotation) {
        addAnnotation(jandexDeclaration, Annotations.jandexAnnotation(annotation));
    }

    private void removeMatchingAnnotations(JandexDeclaration declaration,
            Predicate<org.jboss.jandex.AnnotationInstance> predicate) {

        if (frozen) {
            throw new IllegalStateException("Annotations transformation frozen");
        }

        org.jboss.jandex.EquivalenceKey key = org.jboss.jandex.EquivalenceKey.of(declaration);

        annotationsOverlay().getAnnotations(declaration, jandexIndex).removeIf(predicate);

        Consumer<TransformationContext> transformation = ctx -> {
            ctx.transform().remove(predicate).done();
        };
        transformations.computeIfAbsent(key, ignored -> new ArrayList<>()).add(transformation);
    }

    void removeAnnotation(JandexDeclaration declaration, Predicate<AnnotationInfo> predicate) {
        org.jboss.jandex.EquivalenceKey key = org.jboss.jandex.EquivalenceKey.of(declaration);

        removeMatchingAnnotations(declaration, new Predicate<org.jboss.jandex.AnnotationInstance>() {
            @Override
            public boolean test(org.jboss.jandex.AnnotationInstance jandexAnnotation) {
                // we only verify the target here because ArC doesn't support annotation transformation
                // on method parameters directly; instead, it must be implemented indirectly by transforming
                // annotations on the _method_
                return key.equals(org.jboss.jandex.EquivalenceKey.of(jandexAnnotation.target()))
                        && predicate.test(new AnnotationInfoImpl(jandexIndex, annotationOverlays, jandexAnnotation));
            }
        });
    }

    void removeAllAnnotations(JandexDeclaration declaration) {
        removeAnnotation(declaration, ignored -> true);
    }

    void freeze() {
        frozen = true;
    }

    // `appliesTo` and `transform` must be overridden for `Parameters`, because ArC doesn't
    // support annotation transformation on method parameters directly; instead, it must be
    // implemented indirectly by transforming annotations on the _method_ (and setting proper
    // annotation target)

    @Override
    public boolean appliesTo(org.jboss.jandex.AnnotationTarget.Kind kind) {
        return this.kind == kind;
    }

    @Override
    public void transform(TransformationContext ctx) {
        JandexDeclaration jandexDeclaration = targetJandexDeclaration(ctx);
        org.jboss.jandex.EquivalenceKey key = org.jboss.jandex.EquivalenceKey.of(jandexDeclaration);
        transformations.getOrDefault(key, Collections.emptyList())
                .forEach(it -> it.accept(ctx));
    }

    abstract JandexDeclaration targetJandexDeclaration(TransformationContext ctx);

    abstract AnnotationsOverlay<JandexDeclaration> annotationsOverlay();

    static class Classes extends AnnotationsTransformation<org.jboss.jandex.ClassInfo> {
        Classes(org.jboss.jandex.IndexView jandexIndex, AllAnnotationOverlays annotationOverlays) {
            super(jandexIndex, annotationOverlays, org.jboss.jandex.AnnotationTarget.Kind.CLASS);
        }

        @Override
        protected org.jboss.jandex.ClassInfo targetJandexDeclaration(
                io.quarkus.arc.processor.AnnotationsTransformer.TransformationContext ctx) {
            return ctx.getTarget().asClass();
        }

        @Override
        AnnotationsOverlay<org.jboss.jandex.ClassInfo> annotationsOverlay() {
            return annotationOverlays.classes;
        }
    }

    static class Methods extends AnnotationsTransformation<org.jboss.jandex.MethodInfo> {
        Methods(org.jboss.jandex.IndexView jandexIndex, AllAnnotationOverlays annotationOverlays) {
            super(jandexIndex, annotationOverlays, org.jboss.jandex.AnnotationTarget.Kind.METHOD);
        }

        @Override
        protected org.jboss.jandex.MethodInfo targetJandexDeclaration(
                io.quarkus.arc.processor.AnnotationsTransformer.TransformationContext ctx) {
            return ctx.getTarget().asMethod();
        }

        @Override
        AnnotationsOverlay<org.jboss.jandex.MethodInfo> annotationsOverlay() {
            return annotationOverlays.methods;
        }
    }

    static class Parameters extends AnnotationsTransformation<org.jboss.jandex.MethodParameterInfo> {
        Parameters(org.jboss.jandex.IndexView jandexIndex, AllAnnotationOverlays annotationOverlays) {
            super(jandexIndex, annotationOverlays, org.jboss.jandex.AnnotationTarget.Kind.METHOD_PARAMETER);
        }

        @Override
        protected org.jboss.jandex.MethodParameterInfo targetJandexDeclaration(
                io.quarkus.arc.processor.AnnotationsTransformer.TransformationContext ctx) {
            // `targetJandexDeclaration` is only called from `super.transform`, which we override here
            throw new UnsupportedOperationException();
        }

        @Override
        AnnotationsOverlay<org.jboss.jandex.MethodParameterInfo> annotationsOverlay() {
            return annotationOverlays.parameters;
        }

        @Override
        public boolean appliesTo(org.jboss.jandex.AnnotationTarget.Kind kind) {
            return org.jboss.jandex.AnnotationTarget.Kind.METHOD == kind;
        }

        @Override
        public void transform(TransformationContext ctx) {
            org.jboss.jandex.MethodInfo jandexMethod = ctx.getTarget().asMethod();
            for (org.jboss.jandex.MethodParameterInfo jandexDeclaration : jandexMethod.parameters()) {
                org.jboss.jandex.EquivalenceKey key = org.jboss.jandex.EquivalenceKey.of(jandexDeclaration);
                super.transformations.getOrDefault(key, Collections.emptyList())
                        .forEach(it -> it.accept(ctx));
            }
        }
    }

    static class Fields extends AnnotationsTransformation<org.jboss.jandex.FieldInfo> {
        Fields(org.jboss.jandex.IndexView jandexIndex, AllAnnotationOverlays annotationOverlays) {
            super(jandexIndex, annotationOverlays, org.jboss.jandex.AnnotationTarget.Kind.FIELD);
        }

        @Override
        protected org.jboss.jandex.FieldInfo targetJandexDeclaration(
                io.quarkus.arc.processor.AnnotationsTransformer.TransformationContext ctx) {
            return ctx.getTarget().asField();
        }

        @Override
        AnnotationsOverlay<org.jboss.jandex.FieldInfo> annotationsOverlay() {
            return annotationOverlays.fields;
        }
    }
}
