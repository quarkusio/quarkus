package io.quarkus.resteasy.server.common.runtime;

import java.util.List;
import java.util.function.Function;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ResteasyServerCommonRecorder {

    public void setupIntegration(BeanContainer container, List<Function<Object, Object>> propertyUnwrappers) {
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
    }
}
