package io.quarkus.resteasy.reactive.common.deployment;

import java.util.function.Function;

import org.jboss.resteasy.reactive.spi.BeanFactory;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.resteasy.reactive.common.runtime.ResteasyReactiveCommonRecorder;

public class QuarkusFactoryCreator implements Function<String, BeanFactory<Object>> {

    final ResteasyReactiveCommonRecorder recorder;
    final BeanContainer beanContainer;

    public QuarkusFactoryCreator(ResteasyReactiveCommonRecorder recorder, BeanContainer beanContainer) {
        this.recorder = recorder;
        this.beanContainer = beanContainer;
    }

    @Override
    public BeanFactory<Object> apply(String s) {
        return recorder.factory(s, beanContainer);
    }
}
