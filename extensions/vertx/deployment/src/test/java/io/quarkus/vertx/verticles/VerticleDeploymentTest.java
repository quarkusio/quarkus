package io.quarkus.vertx.verticles;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.Message;

public class VerticleDeploymentTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap
                    .create(JavaArchive.class)
                    .addClasses(MyBeanVerticle.class, VerticleDeployer.class)
                    .addAsResource(new StringAsset("address=foo"), "application.properties"));

    @Inject
    Vertx vertx;

    @Test
    public void test() {
        String s = vertx.eventBus().<String> request("foo", "anyone?")
                .onItem().apply(Message::body)
                .await().indefinitely();
        assertThat(s).isEqualTo("hello");
    }
}
