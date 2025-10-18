package org.acme;

import java.util.Collections;
import java.util.Map;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class ConsulContainerWithFixedPortsTestResource implements QuarkusTestResourceLifecycleManager {

    private GenericContainer<?> containerWithFixedPorts;
    protected static final String IMAGE = "consul:1.7";

    @Override
    public Map<String, String> start() {

        containerWithFixedPorts = new GenericContainer<>(IMAGE)
                .withCreateContainerCmdModifier(cmd -> {
                    HostConfig hostConfig = new HostConfig()
                            .withPortBindings(
                                    new PortBinding(Ports.Binding.bindPort(8500), new ExposedPort(8500)),
                                    new PortBinding(Ports.Binding.bindPort(8501), new ExposedPort(8501)));
                    cmd.withHostConfig(hostConfig);
                })
                .withExposedPorts(8500, 8501)
                .withCommand("agent", "-dev", "-client=0.0.0.0", "-bind=0.0.0.0", "--https-port=8501")
                .waitingFor(Wait.forLogMessage(".*Synced node info.*", 1));
        containerWithFixedPorts.start();

        return Collections.emptyMap();

    }

    @Override
    public void stop() {
        containerWithFixedPorts.stop();
    }

}
