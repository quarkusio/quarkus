package io.quarkus.signals.runtime.impl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import jakarta.enterprise.inject.spi.InjectionPoint;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.signals.Signal;

public class SignalBeanCreator implements BeanCreator<Object> {

    @Override
    public Object create(SyntheticCreationalContext<Object> context) {
        InjectionPoint ip = context.getInjectedReference(InjectionPoint.class);
        ReceiverManager manager = context.getInjectedReference(ReceiverManager.class);
        Type type = ip.getType();
        if (type instanceof ParameterizedType pt && pt.getRawType().equals(Signal.class)) {
            type = pt.getActualTypeArguments()[0];
        }
        return new SignalImpl<>(type, ip.getQualifiers(), Map.of(), manager);
    }

}
