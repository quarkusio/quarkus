package io.quarkus.smallrye.reactivemessaging.amqp.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

public final class DevServicesAmqpBrokerBuildItem extends SimpleBuildItem {

    public final String host;
    public final int port;
    public final String user;
    public final String password;

    public DevServicesAmqpBrokerBuildItem(String host, int port, String user, String password) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
    }
}
