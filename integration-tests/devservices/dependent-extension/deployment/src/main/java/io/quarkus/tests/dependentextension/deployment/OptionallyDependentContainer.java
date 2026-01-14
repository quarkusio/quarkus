package io.quarkus.tests.dependentextension.deployment;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.deployment.builditem.Startable;

public class OptionallyDependentContainer extends GenericContainer<OptionallyDependentContainer>
        implements Startable {

    private static final DockerImageName dockerImageName = DockerImageName.parse("httpd");
    public static final int HTTPD_PORT = 80;
    private String otherConfig;

    public OptionallyDependentContainer() {
        super(dockerImageName);
        this //.waitingFor(Wait.forLogMessage(".*" + "resuming normal operations" + ".*", 1))
                .withReuse(true)
                .withExposedPorts(HTTPD_PORT);
    }

    @Override
    public void start() {
        // Start, even if the config is not available
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

    public void setOtherUrl(String value) {
        if (otherConfig == null) {
            throw new IllegalArgumentException("Should not attempt to set other config if it is not available");
        }
        otherConfig = value;
    }

    public boolean isDependencyAvailable() {
        return otherConfig != null;
    }
}
