package io.quarkus.smallrye.reactivemessaging.rabbitmq.runtime.dev.ui;

import java.util.function.Supplier;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.impl.LazyValue;

public class DevRabbitMqHttpPort {

    private final LazyValue<String> httpPort;

    public DevRabbitMqHttpPort() {
        this.httpPort = new LazyValue<>(new Supplier<String>() {

            @Override
            public String get() {
                ArcContainer arcContainer = Arc.container();
                RabbitHttpPortFinder rabbitHttpPortFinder = arcContainer.instance(RabbitHttpPortFinder.class).get();

                return rabbitHttpPortFinder.httpPort;
            }
        });
    }

    public String getHttpPort() {
        return httpPort.get();
    }
}
