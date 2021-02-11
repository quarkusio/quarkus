package io.quarkus.arquillian;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

public class QuarkusConfiguration implements ContainerConfiguration {

    @Override
    public void validate() throws ConfigurationException {
    }

}
