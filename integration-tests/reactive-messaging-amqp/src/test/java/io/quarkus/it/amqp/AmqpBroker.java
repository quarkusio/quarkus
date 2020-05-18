package io.quarkus.it.amqp;

import static org.awaitility.Awaitility.await;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.activemq.artemis.core.security.CheckType;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class AmqpBroker implements QuarkusTestResourceLifecycleManager {

    static EmbeddedActiveMQ server;
    static AmqpBroker instance;

    public Map<String, String> start() {
        instance = this;
        try {
            server = new EmbeddedActiveMQ();
            server.setSecurityManager(new ActiveMQSecurityManager() {
                @Override
                public boolean validateUser(String username, String password) {
                    return username.equalsIgnoreCase("artemis") && password.equalsIgnoreCase("artemis");
                }

                @Override
                public boolean validateUserAndRole(String username, String password, Set<Role> set,
                        CheckType checkType) {
                    return username.equalsIgnoreCase("artemis") && password.equalsIgnoreCase("artemis");
                }
            });
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        await().until(() -> server.getActiveMQServer() != null
                && server.getActiveMQServer().isActive()
                && server.getActiveMQServer().getConnectorsService().isStarted());
        return Collections.emptyMap();
    }

    public void stop() {
        try {
            if (server != null) {
                server.stop();
            }
            instance = null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void restartBroker() {
        if (instance != null) {
            instance.stop();
        }
        new AmqpBroker().start();
    }

}
