package io.quarkus.arc.processor.bcextensions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.AnnotationMember;
import jakarta.enterprise.lang.model.declarations.ClassInfo;

import org.jboss.jandex.DotName;

class AnnotationInfoImpl implements AnnotationInfo {
    final org.jboss.jandex.IndexView jandexIndex;
    final org.jboss.jandex.MutableAnnotationOverlay annotationOverlay;
    final org.jboss.jandex.AnnotationInstance jandexAnnotation;

    AnnotationInfoImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            org.jboss.jandex.AnnotationInstance jandexAnnotation) {
        this.jandexIndex = jandexIndex;
        this.annotationOverlay = annotationOverlay;
        this.jandexAnnotation = jandexAnnotation;
    }

    @Override
    public ClassInfo declaration() {
        DotName annotationClassName = jandexAnnotation.name();
        org.jboss.jandex.ClassInfo annotationClass = jandexIndex.getClassByName(annotationClassName);
        if (annotationClass == null) {
            throw new IllegalStateException("Class " + annotationClassName + " not found in Jandex");
        }
        return new ClassInfoImpl(jandexIndex, annotationOverlay, annotationClass);
    }

    @Override
    public boolean hasMember(String name) {
        return jandexAnnotation.valueWithDefault(jandexIndex, name) != null;
    }

    @Override
    public AnnotationMember member(String name) {
        return new AnnotationMemberImpl(jandexIndex, annotationOverlay,
                jandexAnnotation.valueWithDefault(jandexIndex, name));
    }

    @Override
    public Map<String, AnnotationMember> members() {
        Map<String, AnnotationMember> result = new HashMap<>();
        for (org.jboss.jandex.AnnotationValue jandexAnnotationMember : jandexAnnotation.valuesWithDefaults(jandexIndex)) {
            result.put(jandexAnnotationMember.name(),
                    new AnnotationMemberImpl(jandexIndex, annotationOverlay, jandexAnnotationMember));
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AnnotationInfoImpl that = (AnnotationInfoImpl) o;
        return Objects.equals(jandexAnnotation.name(), that.jandexAnnotation.name())
                && Objects.equals(members(), that.members());
    }

    @Override
    public int hashCode() {
        return Objects.hash(jandexAnnotation.name(), members());
    }

    @Override
    public String toString() {
        return jandexAnnotation.toString(false);
    }
}
