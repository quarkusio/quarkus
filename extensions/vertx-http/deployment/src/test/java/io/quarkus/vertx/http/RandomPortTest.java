package io.quarkus.vertx.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.cors.BeanRegisteringRoute;

public class RandomPortTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(BeanRegisteringRoute.class)
                    .addAsResource(new StringAsset("quarkus.http.test-port=0"),
                            "application.properties"));

    @TestHTTPResource("test")
    URL url;

    @Test
    public void portShouldNotBeZero() {
        assertThat(url.getPort()).isNotZero();
    }

}
