package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;

public class BasicRestClientTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloClient.class, HelloResource.class, TestBean.class, HelloClient2.class))
            .withConfigurationResource("basic-test-application.properties");

    @Inject
    TestBean testBean;

    @Test
    void shouldHello() {
        assertThat(testBean.helloViaBuiltClient("w0rld")).isEqualTo("hello, w0rld");
    }

    @Test
    void shouldHelloThroughInjectedClient() {
        assertThat(testBean.helloViaInjectedClient("wor1d")).isEqualTo("hello, wor1d");
    }

    @Test
    void shouldHaveApplicationScopeByDefault() {
        BeanManager beanManager = Arc.container().beanManager();
        Set<Bean<?>> beans = beanManager.getBeans(HelloClient2.class, RestClient.LITERAL);
        Bean<?> resolvedBean = beanManager.resolve(beans);
        assertThat(resolvedBean.getScope()).isEqualTo(ApplicationScoped.class);
    }

    @Test
    void shouldInvokeClientResponseOnSameContext() {
        assertThat(testBean.bug18977()).isEqualTo("Hello");
    }
}
