package io.quarkus.arc.arquillian;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

public class ArcContainerConfiguration implements ContainerConfiguration {
    @Override
    public void validate() throws ConfigurationException {
    }
}
