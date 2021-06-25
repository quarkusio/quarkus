package io.quarkus.redis.client.deployment;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import io.smallrye.common.constraint.Nullable;
import org.jboss.logging.Logger;
import org.testcontainers.DockerClientFactory;

import java.util.List;

public class ContainerLocator {

    private static final Logger log = Logger.getLogger(DevServicesProcessor.class);

    private final String devServiceLabel;
    private final int port;

    public ContainerLocator(String devServiceLabel, int port) {
        this.devServiceLabel = devServiceLabel;
        this.port = port;
    }

    @Nullable
    private Container lookup(String expectedLabelValue) {
        List<Container> containers = DockerClientFactory.lazyClient().listContainersCmd().exec();
        for (Container container : containers) {
            String s = container.getLabels().get(devServiceLabel);
            if (expectedLabelValue.equalsIgnoreCase(s)) {
                return container;
            }
        }
        return null;
    }

    @Nullable
    private ContainerPort getMappedPort(Container container, int port) {
        for (ContainerPort p : container.getPorts()) {
            Integer mapped = p.getPrivatePort();
            Integer publicPort = p.getPublicPort();
            if (mapped != null && mapped == port && publicPort != null) {
                return p;
            }
        }
        return null;
    }

    @Nullable
    public String locateContainer(String serviceName) {
        Container container = lookup(serviceName);
        if (container != null) {
            ContainerPort containerPort = getMappedPort(container, port);
            if (containerPort != null) {
                String url = containerPort.getIp() + ":" + containerPort.getPublicPort();
                log.infof("Dev Services container locator found: %s (%s). "
                                + "Connecting to: %s.",
                        container.getId(),
                        container.getImage(), url);
                return url;
            }
        }
        return null;
    }
}
