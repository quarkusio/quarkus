package io.quarkus.vertx.http.shutdown;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * Tests that shutdown will wait for current requests to finish.
 *
 * This test records the current time, then sends a request to an endpoint that will take 5s to finish.
 *
 * After undeploy we verify that at least 5s has elapsed, which verifies that the shutdown wait time
 * has worked correctly
 */
public class ShutdownTest {

    protected static final int HANDLER_WAIT_TIME = 5000;

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setAllowTestClassOutsideDeployment(true)
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(ShutdownTest.class)
                            .addAsResource(new StringAsset("quarkus.shutdown.timeout=60"), "application.properties");
                }
            })
            .setAfterUndeployListener(new Runnable() {
                @Override
                public void run() {
                    try {
                        ShutdownTimer.socket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Assertions.assertTrue(System.currentTimeMillis() - ShutdownTimer.requestStarted >= HANDLER_WAIT_TIME);
                    Assertions.assertTrue(System.currentTimeMillis() - ShutdownTimer.requestStarted < 50000); //make sure it did not time out
                }
            });

    @TestHTTPResource
    URL url;

    @Test
    public void testShutdownBehaviour() throws Exception {
        ShutdownTimer.requestStarted = System.currentTimeMillis();
        try {

            ShutdownTimer.socket = new Socket(url.getHost(), url.getPort());
        } catch (IOException e) {
            throw new RuntimeException("Failed to connect to " + url.getHost() + ":" + url.getPort());
        }
        ShutdownTimer.socket.getOutputStream()
                .write("GET /shutdown HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        ShutdownTimer.socket.getOutputStream().flush();
        Thread.sleep(1000);
    }

    @ApplicationScoped
    public static class ShutdownHandler {

        public void setup(@Observes Router router) {
            router.get("/shutdown").handler(new Handler<RoutingContext>() {
                @Override
                public void handle(RoutingContext routingContext) {
                    routingContext.vertx().setTimer(HANDLER_WAIT_TIME, new Handler<Long>() {
                        @Override
                        public void handle(Long aLong) {
                            routingContext.response().end();
                        }
                    });
                }
            });
        }

    }
}
