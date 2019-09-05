package io.quarkus.it.artemis;

import java.util.Random;

import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientSession;

public interface ArtemisHelper {

    default String createBody() {
        return Integer.toString(new Random().nextInt(Integer.MAX_VALUE), 16);
    }

    default ClientSession createSession() throws Exception {
        return ActiveMQClient.createServerLocator("tcp://localhost:61616").createSessionFactory().createSession();
    }
}
