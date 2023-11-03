package io.quarkus.it.vertx.verticles;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

public class MdcVerticle extends AbstractVerticle {
    public static final String MDC_KEY = "fieldKey";
    private static final Logger LOGGER = Logger.getLogger(MdcVerticle.class);

    @Override
    public void start(Promise<Void> done) {
        String address = config().getString("id");
        vertx.eventBus().<String> consumer(address)
                .handler(message -> {
                    MDC.put(MDC_KEY, message.body());
                    LOGGER.warn("Received message ### " + MDC.get(MDC_KEY));
                    vertx.setTimer(50, l -> {
                        LOGGER.warn("Timer fired ### " + MDC.get(MDC_KEY));
                        vertx.executeBlocking(() -> {
                            LOGGER.warn("Blocking task executed ### " + MDC.get(MDC_KEY));
                            return null;
                        }).onComplete(bar -> message.reply("OK-" + MDC.get(MDC_KEY)));
                    });
                })
                .completionHandler(done);
    }
}