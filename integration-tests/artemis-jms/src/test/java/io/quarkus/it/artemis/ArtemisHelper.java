package io.quarkus.it.artemis;

import java.util.Random;

import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory;

public interface ArtemisHelper {

    default String createBody() {
        return Integer.toString(new Random().nextInt(Integer.MAX_VALUE), 16);
    }

    default JMSContext createContext() throws JMSException {
        return new ActiveMQJMSConnectionFactory("tcp://localhost:61616").createContext(Session.AUTO_ACKNOWLEDGE);
    }
}
