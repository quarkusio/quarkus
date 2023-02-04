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
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
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

        private HttpRequest<JsonObject> request;

        @Override
        public void start(Promise<Void> startPromise) {
            WebClient webClient = WebClient.create(vertx);
            request = webClient.getAbs("http://worldclockapi.com/api/json/utc/now").as(BodyCodec.jsonObject());

            Promise<HttpServer> httpServerPromise = Promise.promise();
            httpServerPromise.future().<Void> mapEmpty().onComplete(startPromise);
            vertx.createHttpServer()
                    .requestHandler(req -> {
                        String requestId = req.getHeader(REQUEST_ID_HEADER);

                        MDC.put(MDC_KEY, requestId);
                        LOGGER.info("Received HTTP request ### " + MDC.get(MDC_KEY));

                        vertx.setTimer(50, l -> {
                            LOGGER.info("Timer fired ### " + MDC.get(MDC_KEY));
                            vertx.executeBlocking(fut -> {
                                LOGGER.info("Blocking task executed ### " + MDC.get(MDC_KEY));
                                fut.complete();
                            }, false, bar -> request.send(rar -> {
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
