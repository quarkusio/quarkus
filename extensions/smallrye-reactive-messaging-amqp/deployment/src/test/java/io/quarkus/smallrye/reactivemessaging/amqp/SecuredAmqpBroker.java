package io.quarkus.smallrye.reactivemessaging.amqp;

import static org.awaitility.Awaitility.await;

import java.util.Set;

import org.apache.activemq.artemis.core.security.CheckType;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager;

public class SecuredAmqpBroker {

    private static EmbeddedActiveMQ server;

    private SecuredAmqpBroker() {
        // avoid direct instantiation.
    }

    public static void start() {
        try {
            server = new EmbeddedActiveMQ();
            server.setSecurityManager(new ActiveMQSecurityManager() {
                @Override
                public boolean validateUser(String username, String password) {
                    return username.equalsIgnoreCase("artemis") && password.equalsIgnoreCase("artemis");
                }

                @Override
                public boolean validateUserAndRole(String username, String password, Set<Role> set, CheckType checkType) {
                    return username.equalsIgnoreCase("artemis") && password.equalsIgnoreCase("artemis");
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
