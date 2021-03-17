package io.quarkus.resteasy.common.runtime;

import java.util.List;
import java.util.function.Function;

import org.jboss.resteasy.spi.InjectorFactory;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ResteasyInjectorFactoryRecorder {

    public RuntimeValue<InjectorFactory> setup(BeanContainer container, List<Function<Object, Object>> propertyUnwrappers) {
        QuarkusInjectorFactory.CONTAINER = container;
        QuarkusInjectorFactory.PROXY_UNWRAPPER = new Function<Object, Object>() {
            @Override
            public Object apply(Object o) {
                Object res = o;
                for (Function<Object, Object> i : propertyUnwrappers) {
                    res = i.apply(res);
                }
                return res;
            }
        };
        return new RuntimeValue<>(new QuarkusInjectorFactory());
    }
}
