package io.quarkus.arc.processor.bcextensions;

import java.lang.annotation.Annotation;
import java.util.function.Predicate;

import jakarta.enterprise.inject.build.compatible.spi.DeclarationConfig;
import jakarta.enterprise.lang.model.AnnotationInfo;

abstract class DeclarationConfigImpl<JandexDeclaration extends org.jboss.jandex.Declaration, THIS extends DeclarationConfigImpl<JandexDeclaration, THIS>>
        implements DeclarationConfig {
    final org.jboss.jandex.IndexView jandexIndex;
    final org.jboss.jandex.MutableAnnotationOverlay annotationOverlay;
    final JandexDeclaration jandexDeclaration;

    DeclarationConfigImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            JandexDeclaration jandexDeclaration) {
        this.jandexIndex = jandexIndex;
        this.annotationOverlay = annotationOverlay;
        this.jandexDeclaration = jandexDeclaration;
    }

    @Override
    public THIS addAnnotation(Class<? extends Annotation> annotationType) {
        annotationOverlay.addAnnotation(jandexDeclaration, org.jboss.jandex.AnnotationInstance.builder(annotationType).build());
        return (THIS) this;
    }

    @Override
    public THIS addAnnotation(AnnotationInfo annotation) {
        annotationOverlay.addAnnotation(jandexDeclaration, ((AnnotationInfoImpl) annotation).jandexAnnotation);
        return (THIS) this;
    }

    @Override
    public THIS addAnnotation(Annotation annotation) {
        annotationOverlay.addAnnotation(jandexDeclaration, io.quarkus.arc.processor.Annotations.jandexAnnotation(annotation));
        return (THIS) this;
    }

    @Override
    public THIS removeAnnotation(Predicate<AnnotationInfo> predicate) {
        annotationOverlay.removeAnnotations(jandexDeclaration, new Predicate<org.jboss.jandex.AnnotationInstance>() {
            @Override
            public boolean test(org.jboss.jandex.AnnotationInstance annotationInstance) {
                return predicate.test(new AnnotationInfoImpl(jandexIndex, annotationOverlay, annotationInstance));
            }
        });
        return (THIS) this;
    }

    @Override
    public THIS removeAllAnnotations() {
        annotationOverlay.removeAnnotations(jandexDeclaration, ignored -> true);
        return (THIS) this;
    }
}
