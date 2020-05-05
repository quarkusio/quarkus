package io.quarkus.smallrye.reactivemessaging.deployment;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.jandex.DotName;

import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.annotations.Broadcast;
import io.smallrye.reactive.messaging.annotations.Channel;
import io.smallrye.reactive.messaging.annotations.Emitter;
import io.smallrye.reactive.messaging.annotations.Merge;
import io.smallrye.reactive.messaging.annotations.OnOverflow;
import io.smallrye.reactive.messaging.metrics.MetricDecorator;

public final class ReactiveMessagingDotNames {

    static final DotName VOID = DotName.createSimple(void.class.getName());
    static final DotName INCOMING = DotName.createSimple(Incoming.class.getName());
    static final DotName OUTGOING = DotName.createSimple(Outgoing.class.getName());
    static final DotName BLOCKING = DotName.createSimple(Blocking.class.getName());
    static final DotName CHANNEL = DotName.createSimple(org.eclipse.microprofile.reactive.messaging.Channel.class.getName());
    static final DotName LEGACY_CHANNEL = DotName.createSimple(Channel.class.getName());
    static final DotName EMITTER = DotName.createSimple(org.eclipse.microprofile.reactive.messaging.Emitter.class.getName());
    static final DotName LEGACY_EMITTER = DotName.createSimple(Emitter.class.getName());
    static final DotName ON_OVERFLOW = DotName
            .createSimple(org.eclipse.microprofile.reactive.messaging.OnOverflow.class.getName());
    static final DotName LEGACY_ON_OVERFLOW = DotName.createSimple(OnOverflow.class.getName());
    static final DotName ACKNOWLEDGMENT = DotName.createSimple(Acknowledgment.class.getName());
    static final DotName MERGE = DotName.createSimple(Merge.class.getName());
    static final DotName BROADCAST = DotName.createSimple(Broadcast.class.getName());

    static final DotName METRIC_DECORATOR = DotName.createSimple(MetricDecorator.class.getName());

    // Used to detect REST endpoints and JAX-RS provider
    public static final DotName JAXRS_PATH = DotName.createSimple("javax.ws.rs.Path");
    public static final DotName REST_CONTROLLER = DotName
            .createSimple("org.springframework.web.bind.annotation.RestController");
    public static final DotName JAXRS_PROVIDER = DotName.createSimple("javax.ws.rs.ext.Provider");

    private ReactiveMessagingDotNames() {
    }

}
