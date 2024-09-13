package io.quarkus.smallrye.reactivemessaging.pulsar.deployment;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import com.github.dockerjava.api.command.InspectContainerResponse;

public class PulsarContainer extends GenericContainer<PulsarContainer> {

    public static final DockerImageName PULSAR_IMAGE = DockerImageName.parse("apachepulsar/pulsar:3.2.4");

    public static final String STARTER_SCRIPT = "/run_pulsar.sh";

    public static final int BROKER_PORT = 6650;
    public static final int BROKER_HTTP_PORT = 8080;

    public PulsarContainer() {
        this(PULSAR_IMAGE);
    }

    public PulsarContainer(DockerImageName imageName) {
        super(imageName);
        super.withExposedPorts(BROKER_PORT, BROKER_HTTP_PORT);
        super.withStartupTimeout(Duration.ofSeconds(60));
        super.waitingFor(Wait.forLogMessage(".*Created namespace public/default.*", 1));
        super.withCommand("sh", "-c", runStarterScript());
        super.withTmpFs(Collections.singletonMap("/pulsar/data", "rw"));
    }

    protected String runStarterScript() {
        return "while [ ! -x " + STARTER_SCRIPT + " ]; do sleep 0.1; done; " + STARTER_SCRIPT;
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo, boolean reused) {
        super.containerIsStarting(containerInfo, reused);
        String advertisedListeners = String.format("internal:pulsar://localhost:%s,external:pulsar://%s:%s",
                BROKER_PORT, this.getHost(), this.getMappedPort(BROKER_PORT));

        String command = "#!/bin/bash \n";
        command += "export PULSAR_PREFIX_advertisedListeners=" + advertisedListeners + " \n";
        command += "bin/apply-config-from-env.py conf/standalone.conf && bin/pulsar standalone -nfw -nss";
        copyFileToContainer(
                Transferable.of(command.getBytes(StandardCharsets.UTF_8), 700),
                STARTER_SCRIPT);
    }

    public PulsarContainer withPort(final int fixedPort) {
        if (fixedPort <= 0) {
            throw new IllegalArgumentException("The fixed port must be greater than 0");
        }
        addFixedExposedPort(fixedPort, BROKER_PORT);
        return self();
    }

    public String getPulsarBrokerUrl() {
        return String.format("pulsar://%s:%s", this.getHost(), this.getMappedPort(BROKER_PORT));
    }

    public String getHttpServiceUrl() {
        return String.format("http://%s:%s", this.getHost(), this.getMappedPort(BROKER_HTTP_PORT));
    }
}
