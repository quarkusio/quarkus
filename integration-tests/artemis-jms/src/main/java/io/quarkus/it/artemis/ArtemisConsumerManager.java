package io.quarkus.it.artemis;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;

@ApplicationScoped
public class ArtemisConsumerManager {

    @Inject
    ConnectionFactory connectionFactory;

    private Connection connection;

    @PostConstruct
    public void init() throws JMSException {
        connection = connectionFactory.createConnection();
        connection.start();
    }

    public String receive() {
        try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            MessageConsumer consumer = session.createConsumer(session.createQueue("test-jms"));
            return consumer.receive(1000L).getBody(String.class);
        } catch (JMSException e) {
            throw new RuntimeException("Could not receive message", e);
        }
    }
}
