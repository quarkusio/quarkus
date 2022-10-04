package io.quarkus.kafka.client.runtime.ui;

import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Handles requests from kafka UI and html/js of UI
 */
@Recorder
public class KafkaUiRecorder {

    public Handler<RoutingContext> kafkaControlHandler() {
        return new KafkaUiHandler();
    }

}
