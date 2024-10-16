package io.quarkus.observability.testcontainers;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.observability.common.config.ContainerConfig;

@SuppressWarnings("resource")
public abstract class ObservabilityContainer<T extends ObservabilityContainer<T, C>, C extends ContainerConfig>
        extends GenericContainer<T> {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Logger dockerLog = LoggerFactory.getLogger(getClass().getName() + ".docker");

    public ObservabilityContainer(C config) {
        super(DockerImageName.parse(config.imageName()));
        withLogConsumer(frameConsumer());
        withLabel(config.label(), config.serviceName());
        Optional<Set<String>> aliases = config.networkAliases();
        aliases.map(s -> s.toArray(new String[0])).ifPresent(this::withNetworkAliases);
        if (config.shared()) {
            withNetwork(Network.SHARED);
        }
    }

    protected Consumer<OutputFrame> frameConsumer() {
        return frame -> logger().debug(frame.getUtf8String().stripTrailing());
    }

    protected byte[] getResourceAsBytes(String resource) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            return in.readAllBytes();
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    @SuppressWarnings("OctalInteger")
    protected void addFileToContainer(byte[] content, String pathInContainer) {
        logger().info("Content [{}]: \n{}", pathInContainer, new String(content, StandardCharsets.UTF_8));
        withCopyToContainer(Transferable.of(content, 0777), pathInContainer);
    }

    @Override
    protected Logger logger() {
        return dockerLog;
    }

    @Override
    public void start() {
        log.info("Starting {} ...", getClass().getSimpleName());
        super.start();
    }

    @Override
    public void stop() {
        log.info("Stopping {}...", getClass().getSimpleName());
        super.stop();
    }
}
