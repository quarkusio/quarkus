package io.quarkus.arc.processor.bcextensions;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.types.Type;

import org.jboss.jandex.DotName;

import io.smallrye.common.annotation.SuppressForbidden;

abstract class TypeImpl<JandexType extends org.jboss.jandex.Type> extends AnnotationTargetImpl implements Type {
    final JandexType jandexType;

    TypeImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            JandexType jandexType) {
        super(jandexIndex, annotationOverlay, org.jboss.jandex.EquivalenceKey.of(jandexType));
        this.jandexType = jandexType;
    }

    static Type fromJandexType(org.jboss.jandex.IndexView jandexIndex,
            org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            org.jboss.jandex.Type jandexType) {
        return switch (jandexType.kind()) {
            case VOID -> new VoidTypeImpl(jandexIndex, annotationOverlay, jandexType.asVoidType());
            case PRIMITIVE -> new PrimitiveTypeImpl(jandexIndex, annotationOverlay, jandexType.asPrimitiveType());
            case CLASS -> new ClassTypeImpl(jandexIndex, annotationOverlay, jandexType.asClassType());
            case ARRAY -> new ArrayTypeImpl(jandexIndex, annotationOverlay, jandexType.asArrayType());
            case PARAMETERIZED_TYPE ->
                new ParameterizedTypeImpl(jandexIndex, annotationOverlay, jandexType.asParameterizedType());
            case TYPE_VARIABLE -> new TypeVariableImpl(jandexIndex, annotationOverlay, jandexType.asTypeVariable());
            case UNRESOLVED_TYPE_VARIABLE ->
                new UnresolvedTypeVariableImpl(jandexIndex, annotationOverlay, jandexType.asUnresolvedTypeVariable());
            case WILDCARD_TYPE -> new WildcardTypeImpl(jandexIndex, annotationOverlay, jandexType.asWildcardType());
            default -> throw new IllegalArgumentException("Unknown type " + jandexType);
        };
    }

    @Override
    public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
        DotName annotationName = DotName.createSimple(annotationType);
        for (org.jboss.jandex.AnnotationInstance annotation : jandexType.annotations()) {
            if (annotation.runtimeVisible() && annotation.name().equals(annotationName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAnnotation(Predicate<AnnotationInfo> predicate) {
        for (org.jboss.jandex.AnnotationInstance annotation : jandexType.annotations()) {
            if (annotation.runtimeVisible()
                    && predicate.test(new AnnotationInfoImpl(jandexIndex, annotationOverlay, annotation))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <T extends Annotation> AnnotationInfo annotation(Class<T> annotationType) {
        org.jboss.jandex.AnnotationInstance jandexAnnotation = jandexType.annotation(DotName.createSimple(annotationType));
        if (jandexAnnotation == null || !jandexAnnotation.runtimeVisible()) {
            return null;
        }
        return new AnnotationInfoImpl(jandexIndex, annotationOverlay, jandexAnnotation);
    }

    @Override
    public <T extends Annotation> Collection<AnnotationInfo> repeatableAnnotation(Class<T> annotationType) {
        List<AnnotationInfo> result = new ArrayList<>();
        for (org.jboss.jandex.AnnotationInstance annotation : jandexType.annotationsWithRepeatable(
                DotName.createSimple(annotationType), jandexIndex)) {
            if (annotation.runtimeVisible()) {
                result.add(new AnnotationInfoImpl(jandexIndex, annotationOverlay, annotation));
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Collection<AnnotationInfo> annotations(Predicate<AnnotationInfo> predicate) {
        List<AnnotationInfo> result = new ArrayList<>();
        for (org.jboss.jandex.AnnotationInstance annotation : jandexType.annotations()) {
            if (annotation.runtimeVisible()) {
                AnnotationInfo annotationInfo = new AnnotationInfoImpl(jandexIndex, annotationOverlay, annotation);
                if (predicate.test(annotationInfo)) {
                    result.add(annotationInfo);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Collection<AnnotationInfo> annotations() {
        List<AnnotationInfo> result = new ArrayList<>();
        for (org.jboss.jandex.AnnotationInstance annotation : jandexType.annotations()) {
            if (annotation.runtimeVisible()) {
                result.add(new AnnotationInfoImpl(jandexIndex, annotationOverlay, annotation));
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    @SuppressForbidden(reason = "Using Type.toString() to build an informative message")
    public String toString() {
        return jandexType.toString();
    }
}
