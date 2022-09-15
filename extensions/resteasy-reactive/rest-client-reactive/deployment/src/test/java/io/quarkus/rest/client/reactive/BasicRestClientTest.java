package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;

public class BasicRestClientTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloClient.class, HelloResource.class, TestBean.class, HelloClient2.class,
                            HelloNonSimpleClient.class))
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

    @Test
    void shouldHelloBytes() {
        assertThat(testBean.helloNonSimpleSyncBytes()).isEqualTo(new byte[] { 1, 2, 3 });
    }

    @Test
    void shouldHelloInts() {
        assertThat(testBean.helloNonSimpleSyncInts()).isEqualTo(new Integer[] { 1, 2, 3 });
    }

    @Test
    void shouldMapQueryParamsWithSpecialCharacters() {
        Map<String, String> map = testBean.helloQueryParamsToMap();
        assertThat(map).size().isEqualTo(6);
        assertThat(map.get("p1")).isEqualTo("1");
        assertThat(map.get("p2")).isEqualTo("2");
        assertThat(map.get("p3")).isEqualTo("3");
        assertThat(map.get("p4")).isEqualTo("4");
        assertThat(map.get("p5")).isEqualTo("5");
        assertThat(map.get("p6")).isEqualTo("6");
    }
}
