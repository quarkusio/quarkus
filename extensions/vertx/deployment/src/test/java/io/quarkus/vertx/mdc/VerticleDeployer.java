package io.quarkus.vertx.mdc;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.client.WebClient;
import io.vertx.mutiny.core.Vertx;

@ApplicationScoped
public class VerticleDeployer {
    public static final String REQUEST_ID_HEADER = "x-request-id";
    public static final String MDC_KEY = "requestId";
    public static final int VERTICLE_PORT = 8097;

    @Inject
    Vertx vertx;

    private volatile String deploymentId;

    void onStart(@Observes StartupEvent ev) {
        deploymentId = vertx.deployVerticle(new TestVerticle()).await().indefinitely();
    }

    void onStop(@Observes ShutdownEvent ev) {
        if (deploymentId != null) {
            vertx.undeploy(deploymentId).await().indefinitely();
        }
    }

    private static class TestVerticle extends AbstractVerticle {
        private static final Logger LOGGER = Logger.getLogger(TestVerticle.class);

        @Override
        public void start(Promise<Void> startPromise) {
            WebClient webClient = WebClient.create(vertx);
            var request = webClient.getAbs("http://localhost:" + VERTICLE_PORT + "/now");

            Promise<HttpServer> httpServerPromise = Promise.promise();
            httpServerPromise.future().<Void> mapEmpty().onComplete(startPromise);
            vertx.createHttpServer()
                    .requestHandler(req -> {

                        if (req.path().equals("/now")) {
                            req.response().end(Long.toString(System.currentTimeMillis()));
                            return;
                        }

                        String requestId = req.getHeader(REQUEST_ID_HEADER);
                        MDC.put(MDC_KEY, requestId);
                        LOGGER.info("Received HTTP request ### " + MDC.get(MDC_KEY));
                        vertx.setTimer(50, l -> {
                            LOGGER.info("Timer fired ### " + MDC.get(MDC_KEY));
                            vertx.executeBlocking(() -> {
                                LOGGER.info("Blocking task executed ### " + MDC.get(MDC_KEY));
                                return null;
                            }, false).onComplete(bar -> request.send(rar -> {
                                String value = (String) MDC.get(MDC_KEY);
                                LOGGER.info("Received Web Client response ### " + value);
                                req.response().end(value);
                            }));
                        });
                    })
                    .listen(VERTICLE_PORT, httpServerPromise);
        }
    }
}
