package io.quarkus.arc.processor.bcextensions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.declarations.MethodInfo;
import jakarta.enterprise.lang.model.declarations.ParameterInfo;
import jakarta.enterprise.lang.model.types.Type;
import jakarta.enterprise.lang.model.types.TypeVariable;

import org.jboss.jandex.DotName;

class MethodInfoImpl extends DeclarationInfoImpl<org.jboss.jandex.MethodInfo> implements MethodInfo {
    MethodInfoImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            org.jboss.jandex.MethodInfo jandexDeclaration) {
        super(jandexIndex, annotationOverlay, jandexDeclaration);
    }

    @Override
    public String name() {
        if (isConstructor()) {
            return jandexDeclaration.declaringClass().name().toString();
        }
        return jandexDeclaration.name();
    }

    @Override
    public List<ParameterInfo> parameters() {
        List<ParameterInfo> result = new ArrayList<>(jandexDeclaration.parametersCount());
        for (org.jboss.jandex.MethodParameterInfo jandexParameter : jandexDeclaration.parameters()) {
            result.add(new ParameterInfoImpl(jandexIndex, annotationOverlay, jandexParameter));
        }
        return result;
    }

    @Override
    public Type returnType() {
        if (isConstructor()) {
            // Jandex returns a void type as a return type of a constructor,
            // but it has the correct (type use) annotations
            //
            // so we just copy those annotations to a class type
            org.jboss.jandex.AnnotationInstance[] typeAnnotations = jandexDeclaration.returnType().annotations()
                    .toArray(new org.jboss.jandex.AnnotationInstance[0]);
            org.jboss.jandex.Type classType = org.jboss.jandex.Type.createWithAnnotations(
                    jandexDeclaration.declaringClass().name(), org.jboss.jandex.Type.Kind.CLASS, typeAnnotations);
            return TypeImpl.fromJandexType(jandexIndex, annotationOverlay, classType);
        }
        return TypeImpl.fromJandexType(jandexIndex, annotationOverlay, jandexDeclaration.returnType());
    }

    @Override
    public Type receiverType() {
        // static method
        if (Modifier.isStatic(jandexDeclaration.flags())) {
            return null;
        }

        // constructor of a top-level class or a static nested class
        if (MethodPredicates.IS_CONSTRUCTOR_JANDEX.test(jandexDeclaration)) {
            org.jboss.jandex.ClassInfo declaringClass = jandexDeclaration.declaringClass();
            org.jboss.jandex.ClassInfo.NestingType nestingType = declaringClass.nestingType();
            if (nestingType == org.jboss.jandex.ClassInfo.NestingType.TOP_LEVEL) {
                return null;
            }
            if (nestingType == org.jboss.jandex.ClassInfo.NestingType.INNER
                    && Modifier.isStatic(declaringClass.flags())) {
                return null;
            }
        }

        return TypeImpl.fromJandexType(jandexIndex, annotationOverlay, jandexDeclaration.receiverType());
    }

    @Override
    public List<Type> throwsTypes() {
        return jandexDeclaration.exceptions()
                .stream()
                .map(it -> TypeImpl.fromJandexType(jandexIndex, annotationOverlay, it))
                .toList();
    }

    @Override
    public List<TypeVariable> typeParameters() {
        return jandexDeclaration.typeParameters()
                .stream()
                .map(it -> TypeImpl.fromJandexType(jandexIndex, annotationOverlay, it))
                .filter(Type::isTypeVariable) // not necessary, just as a precaution
                .map(Type::asTypeVariable) // not necessary, just as a precaution
                .toList();
    }

    @Override
    public boolean isConstructor() {
        return MethodPredicates.IS_CONSTRUCTOR_JANDEX.test(jandexDeclaration);
    }

    @Override
    public boolean isStatic() {
        return Modifier.isStatic(jandexDeclaration.flags());
    }

    @Override
    public boolean isAbstract() {
        return Modifier.isAbstract(jandexDeclaration.flags());
    }

    @Override
    public boolean isFinal() {
        return Modifier.isFinal(jandexDeclaration.flags());
    }

    @Override
    public int modifiers() {
        return jandexDeclaration.flags();
    }

    @Override
    public ClassInfo declaringClass() {
        return new ClassInfoImpl(jandexIndex, annotationOverlay, jandexDeclaration.declaringClass());
    }

    @Override
    public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
        DotName annotationName = DotName.createSimple(annotationType);
        for (org.jboss.jandex.AnnotationInstance annotation : annotationOverlay.annotations(jandexDeclaration)) {
            if (annotation.name().equals(annotationName)
                    && annotation.target() != null
                    && annotation.target().kind() == org.jboss.jandex.AnnotationTarget.Kind.METHOD) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAnnotation(Predicate<AnnotationInfo> predicate) {
        for (org.jboss.jandex.AnnotationInstance annotation : annotationOverlay.annotations(jandexDeclaration)) {
            if (predicate.test(new AnnotationInfoImpl(jandexIndex, annotationOverlay, annotation))
                    && annotation.target() != null
                    && annotation.target().kind() == org.jboss.jandex.AnnotationTarget.Kind.METHOD) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <T extends Annotation> AnnotationInfo annotation(Class<T> annotationType) {
        org.jboss.jandex.AnnotationInstance jandexAnnotation = annotationOverlay.annotation(jandexDeclaration, annotationType);
        if (jandexAnnotation == null
                || jandexAnnotation.target() == null
                || jandexAnnotation.target().kind() != org.jboss.jandex.AnnotationTarget.Kind.METHOD) {
            return null;
        }
        return new AnnotationInfoImpl(jandexIndex, annotationOverlay, jandexAnnotation);
    }

    @Override
    public <T extends Annotation> Collection<AnnotationInfo> repeatableAnnotation(Class<T> annotationType) {
        List<AnnotationInfo> result = new ArrayList<>();
        for (org.jboss.jandex.AnnotationInstance annotation : annotationOverlay.annotationsWithRepeatable(jandexDeclaration,
                annotationType)) {
            if (annotation.target() != null
                    && annotation.target().kind() == org.jboss.jandex.AnnotationTarget.Kind.METHOD) {
                result.add(new AnnotationInfoImpl(jandexIndex, annotationOverlay, annotation));
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Collection<AnnotationInfo> annotations(Predicate<AnnotationInfo> predicate) {
        List<AnnotationInfo> result = new ArrayList<>();
        for (org.jboss.jandex.AnnotationInstance annotation : annotationOverlay.annotations(jandexDeclaration)) {
            if (annotation.target() != null
                    && annotation.target().kind() == org.jboss.jandex.AnnotationTarget.Kind.METHOD) {
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
        for (org.jboss.jandex.AnnotationInstance annotation : annotationOverlay.annotations(jandexDeclaration)) {
            if (annotation.target() != null
                    && annotation.target().kind() == org.jboss.jandex.AnnotationTarget.Kind.METHOD) {
                result.add(new AnnotationInfoImpl(jandexIndex, annotationOverlay, annotation));
            }
        }
        return Collections.unmodifiableList(result);
    }
}
