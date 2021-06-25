package io.quarkus.devservices.common;

import java.util.List;

import org.jboss.logging.Logger;
import org.testcontainers.DockerClientFactory;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;

public class ContainerLocator {

    private static final Logger log = Logger.getLogger(ContainerLocator.class);

    private final String devServiceLabel;
    private final int port;

    public ContainerLocator(String devServiceLabel, int port) {
        this.devServiceLabel = devServiceLabel;
        this.port = port;
    }

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
