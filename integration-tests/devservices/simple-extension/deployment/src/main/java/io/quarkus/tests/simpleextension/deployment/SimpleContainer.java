package io.quarkus.tests.simpleextension.deployment;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.deployment.builditem.Startable;

public class SimpleContainer extends GenericContainer<io.quarkus.tests.simpleextension.deployment.SimpleContainer>
        implements Startable {

    private static final DockerImageName dockerImageName = DockerImageName.parse("httpd");
    public static final int HTTPD_PORT = 80;

    public SimpleContainer() {
        super(dockerImageName);
        this //.waitingFor(Wait.forLogMessage(".*" + "resuming normal operations" + ".*", 1))
                .withReuse(true)
                .withExposedPorts(HTTPD_PORT);
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public String getConnectionInfo() {
        return "http://" + getHost() + ":" + getMappedPort(HTTPD_PORT);
    }

    @Override
    public void close() {
        super.close();
    }
}
