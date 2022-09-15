package io.quarkus.vertx.verticles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.inject.Inject;

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
                    .addClasses(MyBeanVerticle.class, MyUndeployedVerticle.class, NotDeployedVerticle.class,
                            VerticleDeployer.class)
                    .addAsResource(new StringAsset("address=foo"), "application.properties"));

    @Inject
    Vertx vertx;

    @Inject
    VerticleDeployer deployer;

    @Test
    public void test() {
        String s = vertx.eventBus().<String> request("foo", "anyone?")
                .onItem().transform(Message::body)
                .await().indefinitely();
        assertThat(s).isEqualTo("hello");

        // No handlers for address alpha - NotDeployedVerticle was not deployed
        assertNull(vertx.eventBus().<String> request("alpha", "anyone?")
                .onFailure().recoverWithItem(() -> null)
                .await().indefinitely());

        // Handled by MyUndeployedVerticle
        assertThat(vertx.eventBus().<String> request("bravo", "anyone?")
                .onItem().transform(Message::body)
                .await().indefinitely()).isEqualTo("hello from bravo");

        // Undeploy MyUndeployedVerticle manually
        deployer.undeploy();
    }
}
