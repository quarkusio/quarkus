package io.quarkus.smallrye.reactivemessaging.amqp;

import static org.awaitility.Awaitility.await;

import java.util.Set;

import org.apache.activemq.artemis.core.security.CheckType;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager;

public class AnonymousAmqpBroker {

    private static EmbeddedActiveMQ server;

    private AnonymousAmqpBroker() {
        // avoid direct instantiation.
    }

    public static void start() {
        try {
            server = new EmbeddedActiveMQ();
            server.setSecurityManager(new ActiveMQSecurityManager() {
                @Override
                public boolean validateUser(String s, String s1) {
                    return true;
                }

                @Override
                public boolean validateUserAndRole(String s, String s1, Set<Role> set, CheckType checkType) {
                    return true;
                }
            });
            server.start();
            await().until(() -> server.getActiveMQServer().isStarted());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void stop() {
        try {
            if (server != null) {
                server.stop();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
