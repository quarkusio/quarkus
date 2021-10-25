package io.quarkus.smallrye.reactivemessaging.rabbitmq.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

public final class DevServicesRabbitMQBrokerBuildItem extends SimpleBuildItem {

    public final String host;
    public final int port;
    public final String user;
    public final String password;

    public DevServicesRabbitMQBrokerBuildItem(String host, int port, String user, String password) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
    }
}
