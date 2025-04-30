package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.rest.client.reactive.configuration.EchoResource;
import io.quarkus.test.QuarkusUnitTest;

public class ClassNameScopeOverrideTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(EchoResource.class, HelloClientWithBaseUri.class))
            .withConfigurationResource("classname-scope-test-application.properties");

    @RestClient
    HelloClientWithBaseUri client;

    @Test
    void shouldHaveDependentScope() {
        BeanManager beanManager = Arc.container().beanManager();
        Set<Bean<?>> beans = beanManager.getBeans(HelloClientWithBaseUri.class, RestClient.LITERAL);
        Bean<?> resolvedBean = beanManager.resolve(beans);
        assertThat(resolvedBean.getScope()).isEqualTo(Dependent.class);
    }

    @Test
    void shouldConnect() {
        assertThat(client.echo("Bob")).isEqualTo("hello, Bob!!!");
    }
}
