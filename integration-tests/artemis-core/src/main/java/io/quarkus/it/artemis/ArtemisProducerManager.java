package io.quarkus.it.artemis;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;

@ApplicationScoped
public class ArtemisProducerManager {

    @Inject
    ServerLocator serverLocator;

    private ClientSessionFactory connection;

    @PostConstruct
    public void init() throws Exception {
        connection = serverLocator.createSessionFactory();
    }

    public void send(String body) {
        try (ClientSession session = connection.createSession()) {
            ClientMessage message = session.createMessage(true);
            message.getBodyBuffer().writeString(body);
            try (ClientProducer producer = session.createProducer("test-core")) {
                producer.send(message);
            }
        } catch (ActiveMQException e) {
            throw new RuntimeException("Could not send message", e);
        }
    }
}
