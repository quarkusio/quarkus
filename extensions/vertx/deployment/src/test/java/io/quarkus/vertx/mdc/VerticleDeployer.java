package io.quarkus.vertx.mdc;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

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

                        MDC.put("requestId", requestId);
                        LOGGER.info("Received HTTP request ### " + requestId);

                        vertx.setTimer(50, l -> {
                            LOGGER.info("Timer fired ### " + requestId);
                            vertx.executeBlocking(fut -> {
                                LOGGER.info("Blocking task executed ### " + requestId);
                                fut.complete();
                            }, false, bar -> request.send(rar -> {
                                LOGGER.info("Received Web Client response ### " + requestId);
                                req.response().end();
                            }));
                        });
                    })
                    .listen(VERTICLE_PORT, httpServerPromise);
        }
    }
}
