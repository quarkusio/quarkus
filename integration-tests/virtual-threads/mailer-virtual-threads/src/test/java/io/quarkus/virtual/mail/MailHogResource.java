package io.quarkus.virtual.mail;

import java.util.Map;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class MailHogResource implements QuarkusTestResourceLifecycleManager {

    private static final String IMAGE = "mailhog/mailhog";
    private static final int SMTP_PORT = 1025;
    private static final int HTTP_PORT = 8025;
    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {
        container = new GenericContainer<>(IMAGE)
                .withExposedPorts(1025, 8025)
                .waitingFor(new LogMessageWaitStrategy().withTimes(1).withRegEx(".*Serving under.*"));
        container.start();

        return Map.of("quarkus.mailer.host", container.getHost(),
                "quarkus.mailer.port", Integer.toString(container.getMappedPort(SMTP_PORT)),
                "mailhog.url", "http://" + container.getHost() + ":" + container.getMappedPort(HTTP_PORT) + "/api/v2/messages");
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }
}
