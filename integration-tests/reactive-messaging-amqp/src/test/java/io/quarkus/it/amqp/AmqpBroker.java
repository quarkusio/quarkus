package io.quarkus.it.amqp;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.activemq.artemis.core.security.CheckType;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class AmqpBroker implements QuarkusTestResourceLifecycleManager {

    private EmbeddedActiveMQ server;

    public Map<String, String> start() {
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
        return Collections.emptyMap();
    }

    public void stop() {
        try {
            if (server != null) {
                server.stop();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
