package io.quarkus.vertx.deployment;

import java.util.concurrent.CompletionStage;

import org.jboss.jandex.DotName;

import io.quarkus.vertx.ConsumeEvent;
import io.quarkus.vertx.LocalEventBusCodec;
import io.vertx.core.eventbus.Message;

public class VertxConstants {

    static final DotName MESSAGE = DotName.createSimple(Message.class.getName());
    static final DotName RX_MESSAGE = DotName
            .createSimple(io.vertx.reactivex.core.eventbus.Message.class.getName());
    static final DotName AXLE_MESSAGE = DotName
            .createSimple(io.vertx.axle.core.eventbus.Message.class.getName());
    static final DotName COMPLETION_STAGE = DotName.createSimple(CompletionStage.class.getName());
    static final DotName LOCAL_EVENT_BUS_CODEC = DotName.createSimple(LocalEventBusCodec.class.getName());
    static final DotName CONSUME_EVENT = DotName.createSimple(ConsumeEvent.class.getName());
}
