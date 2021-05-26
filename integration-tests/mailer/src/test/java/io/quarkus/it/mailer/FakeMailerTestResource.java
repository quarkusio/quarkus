package io.quarkus.it.mailer;

import java.util.HashMap;
import java.util.Map;

import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class FakeMailerTestResource implements QuarkusTestResourceLifecycleManager {

    public GenericContainer<?> server = new FixedHostPortGenericContainer<>("reachfive/fake-smtp-server:latest")
            .withCommand("node", "index.js", "--headers")
            .withFixedExposedPort(9155, 1080)
            .withFixedExposedPort(9156, 1025)
            .waitingFor(Wait.forHttp("/api/emails"));

    @Override
    public Map<String, String> start() {
        server.start();
        HashMap<String, String> properties = new HashMap<>();
        properties.put("quarkus.mailer.port", Integer.toString(server.getMappedPort(1025)));
        properties.put("quarkus.mailer.host", server.getContainerIpAddress());
        properties.put("fake.mailer", server.getContainerIpAddress() + ":" + server.getMappedPort(1080));
        //        properties.put("quarkus.mailer.port", Integer.toString(32787));
        //        properties.put("quarkus.mailer.host", "localhost");
        //        properties.put("fake.mailer", "localhost" + ":" + 32786);
        return properties;
    }

    @Override
    public void stop() {
        server.close();
    }
}
