package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigKeyScopeOverrideTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloResource.class, HelloClient2.class))
            .withConfigurationResource("configkey-scope-test-application.properties");

    @RestClient
    HelloClient2 client;

    @Test
    void shouldHaveDependentScope() {
        BeanManager beanManager = Arc.container().beanManager();
        Set<Bean<?>> beans = beanManager.getBeans(HelloClient2.class, RestClient.LITERAL);
        Bean<?> resolvedBean = beanManager.resolve(beans);
        assertThat(resolvedBean.getScope()).isEqualTo(Dependent.class);
    }

    @Test
    void shouldConnect() {
        assertThat(client.echo("Bob")).isEqualTo("hello, Bob");
    }
}
