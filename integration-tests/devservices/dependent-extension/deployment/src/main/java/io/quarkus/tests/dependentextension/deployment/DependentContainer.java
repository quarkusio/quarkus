package io.quarkus.tests.dependentextension.deployment;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.deployment.builditem.Startable;

public class DependentContainer extends GenericContainer<io.quarkus.tests.dependentextension.deployment.DependentContainer>
        implements Startable {

    private static final DockerImageName dockerImageName = DockerImageName.parse("httpd");
    public static final int HTTPD_PORT = 80;
    private String otherConfig;

    public DependentContainer() {
        super(dockerImageName);
        this //.waitingFor(Wait.forLogMessage(".*" + "resuming normal operations" + ".*", 1))
                .withReuse(true)
                .withExposedPorts(HTTPD_PORT);
    }

    @Override
    public void start() {
        if (otherConfig != null) {
            super.start();
        } else {
            throw new IllegalStateException("Trying to start a dependent container without its dependencies being available");
        }
    }

    @Override
    public String getConnectionInfo() {
        return "http://" + getHost() + ":" + getMappedPort(HTTPD_PORT);
    }

    @Override
    public void close() {
        super.close();
    }

    public boolean isDependencyAvailable() {
        return otherConfig != null;
    }

    public void setOtherUrl(String value) {
        otherConfig = value;
    }
}
