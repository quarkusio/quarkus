package io.quarkus.arc.processor.bcextensions;

import java.util.Collection;

import jakarta.enterprise.event.Reception;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.build.compatible.spi.BeanInfo;
import jakarta.enterprise.inject.build.compatible.spi.ObserverInfo;
import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.declarations.MethodInfo;
import jakarta.enterprise.lang.model.declarations.ParameterInfo;
import jakarta.enterprise.lang.model.types.Type;

class ObserverInfoImpl implements ObserverInfo {
    private final org.jboss.jandex.IndexView jandexIndex;
    private final org.jboss.jandex.MutableAnnotationOverlay annotationOverlay;
    private final io.quarkus.arc.processor.ObserverInfo arcObserverInfo;

    ObserverInfoImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            io.quarkus.arc.processor.ObserverInfo arcObserverInfo) {
        this.jandexIndex = jandexIndex;
        this.annotationOverlay = annotationOverlay;
        this.arcObserverInfo = arcObserverInfo;
    }

    @Override
    public Type eventType() {
        return TypeImpl.fromJandexType(jandexIndex, annotationOverlay, arcObserverInfo.getObservedType());
    }

    @Override
    public Collection<AnnotationInfo> qualifiers() {
        return arcObserverInfo.getQualifiers()
                .stream()
                .map(it -> (AnnotationInfo) new AnnotationInfoImpl(jandexIndex, annotationOverlay, it))
                .toList();
    }

    @Override
    public ClassInfo declaringClass() {
        org.jboss.jandex.ClassInfo jandexClass = jandexIndex.getClassByName(arcObserverInfo.getBeanClass());
        return new ClassInfoImpl(jandexIndex, annotationOverlay, jandexClass);
    }

    @Override
    public MethodInfo observerMethod() {
        if (arcObserverInfo.isSynthetic()) {
            return null;
        }
        return new MethodInfoImpl(jandexIndex, annotationOverlay, arcObserverInfo.getObserverMethod());
    }

    @Override
    public ParameterInfo eventParameter() {
        if (arcObserverInfo.isSynthetic()) {
            return null;
        }
        org.jboss.jandex.MethodParameterInfo jandexParameter = arcObserverInfo.getEventParameter();
        return new ParameterInfoImpl(jandexIndex, annotationOverlay, jandexParameter);
    }

    @Override
    public BeanInfo bean() {
        if (arcObserverInfo.isSynthetic()) {
            return null;
        }
        return BeanInfoImpl.create(jandexIndex, annotationOverlay, arcObserverInfo.getDeclaringBean());
    }

    @Override
    public boolean isSynthetic() {
        return arcObserverInfo.isSynthetic();
    }

    @Override
    public int priority() {
        return arcObserverInfo.getPriority();
    }

    @Override
    public boolean isAsync() {
        return arcObserverInfo.isAsync();
    }

    @Override
    public Reception reception() {
        return arcObserverInfo.getReception();
    }

    @Override
    public TransactionPhase transactionPhase() {
        return arcObserverInfo.getTransactionPhase();
    }

    @Override
    public String toString() {
        return arcObserverInfo.toString();
    }
}
