package io.quarkus.arc.processor.bcextensions;

import java.util.Collection;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.build.compatible.spi.BeanInfo;
import jakarta.enterprise.inject.build.compatible.spi.DisposerInfo;
import jakarta.enterprise.inject.build.compatible.spi.InjectionPointInfo;
import jakarta.enterprise.inject.build.compatible.spi.ScopeInfo;
import jakarta.enterprise.inject.build.compatible.spi.StereotypeInfo;
import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.declarations.FieldInfo;
import jakarta.enterprise.lang.model.declarations.MethodInfo;
import jakarta.enterprise.lang.model.types.Type;

class BeanInfoImpl implements BeanInfo {
    final org.jboss.jandex.IndexView jandexIndex;
    final org.jboss.jandex.MutableAnnotationOverlay annotationOverlay;
    final io.quarkus.arc.processor.BeanInfo arcBeanInfo;

    static BeanInfoImpl create(org.jboss.jandex.IndexView jandexIndex,
            org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            io.quarkus.arc.processor.BeanInfo arcBeanInfo) {
        if (arcBeanInfo.isInterceptor()) {
            return new InterceptorInfoImpl(jandexIndex, annotationOverlay,
                    (io.quarkus.arc.processor.InterceptorInfo) arcBeanInfo);
        }
        return new BeanInfoImpl(jandexIndex, annotationOverlay, arcBeanInfo);
    }

    BeanInfoImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            io.quarkus.arc.processor.BeanInfo arcBeanInfo) {
        this.jandexIndex = jandexIndex;
        this.annotationOverlay = annotationOverlay;
        this.arcBeanInfo = arcBeanInfo;
    }

    @Override
    public ScopeInfo scope() {
        return new ScopeInfoImpl(jandexIndex, annotationOverlay, arcBeanInfo.getScope());
    }

    @Override
    public Collection<Type> types() {
        return arcBeanInfo.getTypes()
                .stream()
                .map(it -> TypeImpl.fromJandexType(jandexIndex, annotationOverlay, it))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Collection<AnnotationInfo> qualifiers() {
        return arcBeanInfo.getQualifiers()
                .stream()
                .map(it -> (AnnotationInfo) new AnnotationInfoImpl(jandexIndex, annotationOverlay, it))
                .toList();
    }

    @Override
    public ClassInfo declaringClass() {
        org.jboss.jandex.ClassInfo beanClass = jandexIndex.getClassByName(arcBeanInfo.getBeanClass());
        return new ClassInfoImpl(jandexIndex, annotationOverlay, beanClass);
    }

    @Override
    public boolean isClassBean() {
        return arcBeanInfo.isClassBean();
    }

    @Override
    public boolean isProducerMethod() {
        return arcBeanInfo.isProducerMethod();
    }

    @Override
    public boolean isProducerField() {
        return arcBeanInfo.isProducerField();
    }

    @Override
    public boolean isSynthetic() {
        return arcBeanInfo.isSynthetic();
    }

    @Override
    public MethodInfo producerMethod() {
        if (arcBeanInfo.isProducerMethod()) {
            return new MethodInfoImpl(jandexIndex, annotationOverlay, arcBeanInfo.getTarget().get().asMethod());
        }
        return null;
    }

    @Override
    public FieldInfo producerField() {
        if (arcBeanInfo.isProducerField()) {
            return new FieldInfoImpl(jandexIndex, annotationOverlay, arcBeanInfo.getTarget().get().asField());
        }
        return null;
    }

    @Override
    public boolean isAlternative() {
        return arcBeanInfo.isAlternative();
    }

    @Override
    public Integer priority() {
        return arcBeanInfo.getPriority();
    }

    @Override
    public String name() {
        return arcBeanInfo.getName();
    }

    @Override
    public DisposerInfo disposer() {
        io.quarkus.arc.processor.DisposerInfo disposer = arcBeanInfo.getDisposer();
        return disposer != null ? new DisposerInfoImpl(jandexIndex, annotationOverlay, disposer) : null;
    }

    @Override
    public Collection<StereotypeInfo> stereotypes() {
        return arcBeanInfo.getStereotypes()
                .stream()
                .map(it -> (StereotypeInfo) new StereotypeInfoImpl(jandexIndex, annotationOverlay, it))
                .toList();
    }

    @Override
    public Collection<InjectionPointInfo> injectionPoints() {
        return arcBeanInfo.getAllInjectionPoints()
                .stream()
                .map(it -> (InjectionPointInfo) new InjectionPointInfoImpl(jandexIndex, annotationOverlay, it))
                .toList();
    }

    @Override
    public String toString() {
        return arcBeanInfo.toString();
    }
}
