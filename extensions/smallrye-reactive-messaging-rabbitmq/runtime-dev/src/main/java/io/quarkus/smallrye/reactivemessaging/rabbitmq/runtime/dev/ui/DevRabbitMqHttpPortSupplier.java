package io.quarkus.smallrye.reactivemessaging.rabbitmq.runtime.dev.ui;

import java.util.function.Supplier;

public class DevRabbitMqHttpPortSupplier implements Supplier<DevRabbitMqHttpPort> {

    @Override
    public DevRabbitMqHttpPort get() {
        return new DevRabbitMqHttpPort();
    }
}
