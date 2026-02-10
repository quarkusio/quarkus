package io.quarkus.tests.simpleextension.deployment;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.deployment.builditem.Startable;

public class SimpleContainer extends GenericContainer<io.quarkus.tests.simpleextension.deployment.SimpleContainer>
        implements Startable {

    private static final DockerImageName dockerImageName = DockerImageName.parse("httpd");
    public static final int HTTPD_PORT = 80;

    private String classLoaderNameOnStart;

    public SimpleContainer() {
        super(dockerImageName);
        this //.waitingFor(Wait.forLogMessage(".*" + "resuming normal operations" + ".*", 1))
                .withReuse(true)
                .withExposedPorts(HTTPD_PORT);
    }

    @Override
    public void start() {
        // At start, the classloader should be the deployment classloader; in normal mode the augmentation classloader also works, but in dev mode the augmentation classloader cannot see application resources
        this.classLoaderNameOnStart = Thread.currentThread().getContextClassLoader().getName();
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

    public String getClassLoaderNameOnStart() {
        return classLoaderNameOnStart;
    }
}
