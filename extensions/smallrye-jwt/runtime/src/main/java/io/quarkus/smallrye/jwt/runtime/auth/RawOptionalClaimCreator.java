package io.quarkus.smallrye.jwt.runtime.auth;

import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.InjectionPoint;

import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.impl.InjectionPointProvider;
import io.smallrye.jwt.auth.cdi.RawClaimTypeProducer;

public class RawOptionalClaimCreator implements BeanCreator<Optional<?>> {

    @Override
    public Optional<?> create(CreationalContext<Optional<?>> creationalContext, Map<String, Object> params) {
        InjectionPoint injectionPoint = InjectionPointProvider.get();
        if (injectionPoint == null) {
            throw new IllegalStateException("No current injection point found");
        }
        RawClaimTypeProducer rawClaimTypeProducer = Arc.container().instance(RawClaimTypeProducer.class).get();
        return rawClaimTypeProducer.getOptionalValue(injectionPoint);
    }

}
