package io.quarkus.spring.di.deployment;

import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import io.quarkus.test.QuarkusUnitTest;

public class BeanWithoutConfigurationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Service.class, SomeConfiguration.class, Consumer.class))
            .setExpectedException(DeploymentException.class);

    @Inject
    Consumer consumer;

    @Test
    public void testValidationFailed() {
        // This method should not be invoked
        Assertions.fail();
    }

    static class Service {
    }

    static class SomeConfiguration {

        @Bean
        public Service consumer() {
            return new Service();
        }
    }

    @Component
    static class Consumer {
        @Autowired
        Service service;
    }

}
