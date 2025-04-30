package io.quarkus.arc.processor.bcextensions;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.enterprise.context.spi.AlterableContext;
import jakarta.enterprise.inject.build.compatible.spi.ClassConfig;
import jakarta.enterprise.inject.build.compatible.spi.MetaAnnotations;

import org.jboss.jandex.DotName;

class MetaAnnotationsImpl implements MetaAnnotations {
    private final org.jboss.jandex.IndexView applicationIndex;
    private final org.jboss.jandex.MutableAnnotationOverlay annotationOverlay;

    private final Map<DotName, ClassConfig> qualifiers;
    private final Map<DotName, ClassConfig> interceptorBindings;
    private final Map<DotName, ClassConfig> stereotypes;
    private final List<ContextData> contexts;

    MetaAnnotationsImpl(org.jboss.jandex.IndexView applicationIndex,
            org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            Map<DotName, ClassConfig> qualifiers, Map<DotName, ClassConfig> interceptorBindings,
            Map<DotName, ClassConfig> stereotypes, List<ContextData> contexts) {
        this.applicationIndex = applicationIndex;
        this.annotationOverlay = annotationOverlay;
        this.qualifiers = qualifiers;
        this.interceptorBindings = interceptorBindings;
        this.stereotypes = stereotypes;
        this.contexts = contexts;
    }

    @Override
    public ClassConfig addQualifier(Class<? extends Annotation> annotation) {
        return addMetaAnnotation(annotation, qualifiers);
    }

    @Override
    public ClassConfig addInterceptorBinding(Class<? extends Annotation> annotation) {
        return addMetaAnnotation(annotation, interceptorBindings);
    }

    @Override
    public ClassConfig addStereotype(Class<? extends Annotation> annotation) {
        return addMetaAnnotation(annotation, stereotypes);
    }

    private ClassConfig addMetaAnnotation(Class<? extends Annotation> annotation, Map<DotName, ClassConfig> map) {
        DotName annotationName = DotName.createSimple(annotation.getName());
        org.jboss.jandex.ClassInfo jandexClass = applicationIndex.getClassByName(annotationName);
        ClassConfig classConfig = new ClassConfigImpl(applicationIndex, annotationOverlay, jandexClass);
        map.put(annotationName, classConfig);
        return classConfig;
    }

    @Override
    public void addContext(Class<? extends Annotation> scopeAnnotation, Class<? extends AlterableContext> contextClass) {
        Objects.requireNonNull(scopeAnnotation);
        Objects.requireNonNull(contextClass);
        contexts.add(new ContextData(contextClass, scopeAnnotation, null));
    }

    @Override
    public void addContext(Class<? extends Annotation> scopeAnnotation, boolean isNormal,
            Class<? extends AlterableContext> contextClass) {
        Objects.requireNonNull(scopeAnnotation);
        Objects.requireNonNull(contextClass);
        contexts.add(new ContextData(contextClass, scopeAnnotation, isNormal));
    }

    static final class ContextData {
        Class<? extends Annotation> scopeAnnotation;
        Boolean isNormal; // null if not set, in which case it's derived from the scope annotation

        Class<? extends AlterableContext> contextClass;

        ContextData(Class<? extends AlterableContext> contextClass, Class<? extends Annotation> scopeAnnotation,
                Boolean isNormal) {
            this.contextClass = contextClass;
            this.scopeAnnotation = scopeAnnotation;
            this.isNormal = isNormal;
        }
    }
}
