package io.quarkus.arc.processor.bcextensions;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanBuilder;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanCreator;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanDisposer;
import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.types.Type;

import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.Types;

class SyntheticBeanBuilderImpl<T> extends SyntheticComponentBuilderBase<SyntheticBeanBuilderImpl<T>>
        implements SyntheticBeanBuilder<T> {
    DotName implementationClass;
    Set<org.jboss.jandex.Type> types = new HashSet<>();
    Set<org.jboss.jandex.AnnotationInstance> qualifiers = new HashSet<>();
    Class<? extends Annotation> scope;
    boolean isAlternative;
    Integer priority;
    String name;
    Set<DotName> stereotypes = new HashSet<>();
    Class<? extends SyntheticBeanCreator<T>> creatorClass;
    Class<? extends SyntheticBeanDisposer<T>> disposerClass;

    SyntheticBeanBuilderImpl(Class<?> implementationClass) {
        this.implementationClass = DotName.createSimple(implementationClass.getName());
    }

    @Override
    SyntheticBeanBuilderImpl<T> self() {
        return this;
    }

    @Override
    public SyntheticBeanBuilder<T> type(Class<?> type) {
        this.types.add(Types.jandexType(type));
        return this;
    }

    @Override
    public SyntheticBeanBuilder<T> type(ClassInfo type) {
        DotName className = ((ClassInfoImpl) type).jandexDeclaration.name();
        this.types.add(org.jboss.jandex.ClassType.create(className));
        return this;
    }

    @Override
    public SyntheticBeanBuilder<T> type(Type type) {
        this.types.add(((TypeImpl<?>) type).jandexType);
        return this;
    }

    @Override
    public SyntheticBeanBuilder<T> qualifier(Class<? extends Annotation> qualifierAnnotation) {
        DotName annotationName = DotName.createSimple(qualifierAnnotation.getName());
        this.qualifiers.add(org.jboss.jandex.AnnotationInstance.create(annotationName, null, AnnotationValueArray.EMPTY));
        return this;
    }

    @Override
    public SyntheticBeanBuilder<T> qualifier(AnnotationInfo qualifierAnnotation) {
        this.qualifiers.add(((AnnotationInfoImpl) qualifierAnnotation).jandexAnnotation);
        return this;
    }

    @Override
    public SyntheticBeanBuilder<T> qualifier(Annotation qualifierAnnotation) {
        this.qualifiers.add(Annotations.jandexAnnotation(qualifierAnnotation));
        return this;
    }

    @Override
    public SyntheticBeanBuilder<T> scope(Class<? extends Annotation> scopeAnnotation) {
        this.scope = scopeAnnotation;
        return this;
    }

    @Override
    public SyntheticBeanBuilder<T> alternative(boolean isAlternative) {
        this.isAlternative = isAlternative;
        return this;
    }

    @Override
    public SyntheticBeanBuilder<T> priority(int priority) {
        this.priority = priority;
        return this;
    }

    @Override
    public SyntheticBeanBuilder<T> name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public SyntheticBeanBuilder<T> stereotype(Class<? extends Annotation> stereotypeAnnotation) {
        this.stereotypes.add(DotName.createSimple(stereotypeAnnotation.getName()));
        return this;
    }

    @Override
    public SyntheticBeanBuilder<T> stereotype(ClassInfo stereotypeAnnotation) {
        this.stereotypes.add(((ClassInfoImpl) stereotypeAnnotation).jandexDeclaration.name());
        return this;
    }

    @Override
    public SyntheticBeanBuilder<T> createWith(Class<? extends SyntheticBeanCreator<T>> creatorClass) {
        this.creatorClass = creatorClass;
        return this;
    }

    @Override
    public SyntheticBeanBuilder<T> disposeWith(Class<? extends SyntheticBeanDisposer<T>> disposerClass) {
        this.disposerClass = disposerClass;
        return this;
    }
}
