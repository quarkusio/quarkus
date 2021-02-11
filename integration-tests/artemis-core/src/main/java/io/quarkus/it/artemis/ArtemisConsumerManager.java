package io.quarkus.it.artemis;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;

@ApplicationScoped
public class ArtemisConsumerManager {

    @Inject
    ServerLocator serverLocator;

    private ClientSessionFactory connection;

    @PostConstruct
    public void init() throws Exception {
        connection = serverLocator.createSessionFactory();
    }

    public String receive() {
        try (ClientSession session = connection.createSession()) {
            session.start();
            try (ClientConsumer consumer = session.createConsumer("test-core")) {
                ClientMessage message = consumer.receive(1000L);
                message.acknowledge();
                return message.getBodyBuffer().readString();
            }
        } catch (ActiveMQException e) {
            throw new RuntimeException("Could not receive message", e);
        }
    }
}
