package io.quarkus.it.artemis;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;

@ApplicationScoped
public class ArtemisProducerManager {

    @Inject
    ConnectionFactory connectionFactory;

    private Connection connection;

    @PostConstruct
    public void init() throws JMSException {
        connection = connectionFactory.createConnection();
    }

    public void send(String body) {
        try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            MessageProducer producer = session.createProducer(session.createQueue("test-jms"));
            producer.send(session.createTextMessage(body));
        } catch (JMSException e) {
            throw new RuntimeException("Could not send message", e);
        }
    }
}
