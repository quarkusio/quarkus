package io.quarkus.smallrye.reactivemessaging.rabbitmq.runtime.dev.ui;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RabbitMqJsonRpcService {

    private String port;

    @PostConstruct
    void init() {
        port = new DevRabbitMqHttpPortSupplier().get().getHttpPort();
    }

    public String getRabbitMqPort() {
        return port;
    }
}
