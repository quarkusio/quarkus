package io.quarkus.arc.processor.bcextensions;

import java.util.Collection;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.build.compatible.spi.InterceptorInfo;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.lang.model.AnnotationInfo;

class InterceptorInfoImpl extends BeanInfoImpl implements InterceptorInfo {
    private final io.quarkus.arc.processor.InterceptorInfo arcInterceptorInfo;

    InterceptorInfoImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            io.quarkus.arc.processor.InterceptorInfo arcInterceptorInfo) {
        super(jandexIndex, annotationOverlay, arcInterceptorInfo);
        this.arcInterceptorInfo = arcInterceptorInfo;
    }

    @Override
    public Integer priority() {
        return arcInterceptorInfo.getPriority();
    }

    @Override
    public Collection<AnnotationInfo> interceptorBindings() {
        return arcInterceptorInfo.getBindings()
                .stream()
                .map(it -> new AnnotationInfoImpl(jandexIndex, annotationOverlay, it))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean intercepts(InterceptionType interceptionType) {
        return arcInterceptorInfo.intercepts(interceptionType);
    }
}
