package io.quarkus.tests.simpleextension.deployment;

import java.util.Optional;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;

import io.quarkus.deployment.builditem.Startable;

public class SimpleContainer extends GenericContainer<io.quarkus.tests.simpleextension.deployment.SimpleContainer>
        implements Startable {

    private static final DockerImageName dockerImageName = DockerImageName.parse("httpd");
    public static final int HTTPD_PORT = 80;

    private String classLoaderNameOnStart;

    public SimpleContainer(Optional<Integer> fixedPort) {
        super(dockerImageName);
        this.withReuse(true);
        //.waitingFor(Wait.forLogMessage(".*" + "resuming normal operations" + ".*", 1)) should work, doesn't :(
        if (fixedPort.isPresent()) {
            this.withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
                    new HostConfig()
                            .withPortBindings(
                                    new PortBinding(
                                            Ports.Binding.bindPort(fixedPort.get()),
                                            new ExposedPort(HTTPD_PORT)))));
        } else {
            this.withExposedPorts(HTTPD_PORT);
        }
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

    @Override
    public String getContainerId() {
        return super.getContainerId();
    }
}
