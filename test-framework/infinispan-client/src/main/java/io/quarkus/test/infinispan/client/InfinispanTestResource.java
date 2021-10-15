package io.quarkus.test.infinispan.client;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.commons.util.Version;
import org.infinispan.server.test.core.InfinispanContainer;
import org.jboss.logging.Logger;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class InfinispanTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = Logger.getLogger(InfinispanTestResource.class);
    public static final String PORT_ARG = "port";
    public static final String USER_ARG = "user";
    public static final String PASSWORD_ARG = "password";
    private static final int DEFAULT_PORT = ConfigurationProperties.DEFAULT_HOTROD_PORT;
    private static final String DEFAULT_USER = "admin";
    private static final String DEFAULT_PASSWORD = "password";
    private static InfinispanContainer INFINISPAN;
    private String USER;
    private String PASSWORD;
    private Integer HOTROD_PORT;

    @Override
    public void init(Map<String, String> initArgs) {
        HOTROD_PORT = Optional.ofNullable(initArgs.get(PORT_ARG)).map(Integer::parseInt).orElse(DEFAULT_PORT);
        USER = Optional.ofNullable(initArgs.get(USER_ARG)).orElse(DEFAULT_USER);
        PASSWORD = Optional.ofNullable(initArgs.get(PASSWORD_ARG)).orElse(DEFAULT_PASSWORD);
    }

    @Override
    public Map<String, String> start() {
        INFINISPAN = new InfinispanContainer();
        INFINISPAN.withUser(USER).withPassword(PASSWORD);
        LOGGER.infof("Starting Infinispan Server %s on port %s with user %s and password %s", Version.getMajorMinor(),
                HOTROD_PORT, USER, PASSWORD);
        INFINISPAN.start();

        final String hosts = INFINISPAN.getContainerIpAddress() + ":" + INFINISPAN.getMappedPort(HOTROD_PORT);
        return Collections.singletonMap("quarkus.infinispan-client.server-list", hosts);
    }

    @Override
    public void stop() {
        if (INFINISPAN != null) {
            INFINISPAN.stop();
        }
    }
}
