package io.quarkus.vertx.http.shutdown;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

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
 * Tests that shutdown will wait for current requests to finish, up to the timeout specified.
 *
 * This test records the current time, then sends a request to an endpoint that will take 50s to finish.
 *
 * After undeploy we verify that less than 50s has elapsed, as the shutdown should have proceeded anyway once
 * the timeout of 100ms was reached.
 */
public class ShutdownTimeoutTest {

    protected static final int HANDLER_WAIT_TIME = 50000;

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setAllowTestClassOutsideDeployment(true)
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(ShutdownTimeoutTest.class)
                            .addAsResource(new StringAsset("quarkus.shutdown.timeout=PT0.1S"), "application.properties");
                }
            })
            .setAfterUndeployListener(new Runnable() {
                @Override
                public void run() {
                    try {
                        ShutdownTimer.socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Assertions.assertTrue(System.currentTimeMillis() - ShutdownTimer.requestStarted < HANDLER_WAIT_TIME);
                }
            });

    @TestHTTPResource
    URL url;

    @Test
    public void testShutdownBehaviour() throws Exception {
        ShutdownTimer.requestStarted = System.currentTimeMillis();
        ShutdownTimer.socket = new Socket(url.getHost(), url.getPort());
        ShutdownTimer.socket.getOutputStream()
                .write("GET /shutdown HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes(StandardCharsets.UTF_8));
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
