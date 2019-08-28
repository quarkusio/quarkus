package io.quarkus.it.artemis;

import java.util.Random;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;

public interface ArtemisHelper {

    default String createBody() {
        return Integer.toString(new Random().nextInt(Integer.MAX_VALUE), 16);
    }

    default Session createSession() throws JMSException {
        Connection connection = new ActiveMQJMSConnectionFactory("tcp://localhost:61616").createConnection();
        connection.start();
        return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }
}
