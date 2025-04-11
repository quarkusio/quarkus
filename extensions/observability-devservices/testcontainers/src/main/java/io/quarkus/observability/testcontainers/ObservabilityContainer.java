package io.quarkus.observability.testcontainers;

import static io.quarkus.devservices.common.Labels.QUARKUS_DEV_SERVICE;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.devservices.common.JBossLoggingConsumer;
import io.quarkus.observability.common.config.ContainerConfig;

@SuppressWarnings("resource")
public abstract class ObservabilityContainer<T extends ObservabilityContainer<T, C>, C extends ContainerConfig>
        extends GenericContainer<T> {

    protected final Logger log = Logger.getLogger(getClass());

    public ObservabilityContainer(C config) {
        super(DockerImageName.parse(config.imageName()));
        withLogConsumer(frameConsumer());
        withLabel(config.label(), config.serviceName());
        withLabel(QUARKUS_DEV_SERVICE, config.serviceName());
        Optional<Set<String>> aliases = config.networkAliases();
        aliases.map(s -> s.toArray(new String[0])).ifPresent(this::withNetworkAliases);
        if (config.shared()) {
            withNetwork(Network.SHARED);
        }
    }

    protected abstract String prefix();

    protected Predicate<OutputFrame> getLoggingFilter() {
        return f -> true;
    }

    protected Consumer<OutputFrame> frameConsumer() {
        return new JBossLoggingConsumer(log)
                .withPrefix(prefix())
                .withLoggingFilter(getLoggingFilter());
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
        log.debugf("Content [%s]: \n%s", pathInContainer, new String(content, StandardCharsets.UTF_8));
        withCopyToContainer(Transferable.of(content, 0777), pathInContainer);
    }

    @Override
    public void start() {
        log.infof("Starting %s ...", prefix());
        super.start();
    }

    @Override
    public void stop() {
        log.infof("Stopping %s...", prefix());
        super.stop();
    }
}
