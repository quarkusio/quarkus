package io.quarkus.resteasy.reactive.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.MalformedURLException;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class BasicMpRestClientTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloClient.class, HelloResource.class, TestBean.class, HelloClient2.class))
            .withConfigurationResource("basic-test-application.properties");

    @Inject
    TestBean testBean;

    @Test
    void shouldHello() {
        assertThat(testBean.helloViaBuiltClient("w0rld")).isEqualTo("hello, w0rld");
    }

    @Test
    void shouldHelloThroughInjectedClient() throws MalformedURLException {
        assertThat(testBean.helloViaInjectedClient("wor1d")).isEqualTo("hello, wor1d");
    }
}
