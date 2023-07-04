package io.quarkus.smallrye.reactivemessaging.deployment;

import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.messaging.spi.OutgoingConnectorFactory;
import org.jboss.jandex.DotName;

import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.reactive.messaging.MessageConverter;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.annotations.Broadcast;
import io.smallrye.reactive.messaging.annotations.Channel;
import io.smallrye.reactive.messaging.annotations.ConnectorAttribute;
import io.smallrye.reactive.messaging.annotations.ConnectorAttributes;
import io.smallrye.reactive.messaging.annotations.Emitter;
import io.smallrye.reactive.messaging.annotations.EmitterFactoryFor;
import io.smallrye.reactive.messaging.annotations.Incomings;
import io.smallrye.reactive.messaging.annotations.Merge;
import io.smallrye.reactive.messaging.annotations.OnOverflow;
import io.smallrye.reactive.messaging.connector.InboundConnector;
import io.smallrye.reactive.messaging.connector.OutboundConnector;
import io.smallrye.reactive.messaging.keyed.KeyValueExtractor;
import io.smallrye.reactive.messaging.keyed.Keyed;
import io.smallrye.reactive.messaging.keyed.KeyedMulti;

public final class ReactiveMessagingDotNames {

    static final DotName VOID = DotName.createSimple(void.class.getName());
    static final DotName VOID_CLASS = DotName.createSimple(Void.class.getName());
    static final DotName OBJECT = DotName.createSimple(Object.class.getName());
    static final DotName COMPLETION_STAGE = DotName.createSimple(CompletionStage.class.getName());
    static final DotName INCOMING = DotName.createSimple(Incoming.class.getName());
    static final DotName INCOMINGS = DotName.createSimple(Incomings.class.getName());
    static final DotName OUTGOING = DotName.createSimple(Outgoing.class.getName());

    public static final DotName CONNECTOR = DotName.createSimple(Connector.class.getName());
    static final DotName CONNECTOR_ATTRIBUTES = DotName.createSimple(ConnectorAttributes.class.getName());
    static final DotName CONNECTOR_ATTRIBUTE = DotName.createSimple(ConnectorAttribute.class.getName());

    static final DotName BLOCKING = DotName.createSimple(Blocking.class.getName());
    public static final DotName CHANNEL = DotName
            .createSimple(org.eclipse.microprofile.reactive.messaging.Channel.class.getName());
    public static final DotName LEGACY_CHANNEL = DotName.createSimple(Channel.class.getName());
    public static final DotName EMITTER = DotName
            .createSimple(org.eclipse.microprofile.reactive.messaging.Emitter.class.getName());
    public static final DotName MUTINY_EMITTER = DotName.createSimple(MutinyEmitter.class.getName());
    public static final DotName LEGACY_EMITTER = DotName.createSimple(Emitter.class.getName());
    static final DotName ON_OVERFLOW = DotName
            .createSimple(org.eclipse.microprofile.reactive.messaging.OnOverflow.class.getName());
    static final DotName LEGACY_ON_OVERFLOW = DotName.createSimple(OnOverflow.class.getName());
    static final DotName ACKNOWLEDGMENT = DotName.createSimple(Acknowledgment.class.getName());
    static final DotName MERGE = DotName.createSimple(Merge.class.getName());
    static final DotName BROADCAST = DotName.createSimple(Broadcast.class.getName());
    static final DotName EMITTER_FACTORY_FOR = DotName.createSimple(EmitterFactoryFor.class.getName());

    static final DotName INCOMING_CONNECTOR_FACTORY = DotName.createSimple(IncomingConnectorFactory.class.getName());
    static final DotName INBOUND_CONNECTOR = DotName.createSimple(InboundConnector.class.getName());
    static final DotName OUTGOING_CONNECTOR_FACTORY = DotName.createSimple(OutgoingConnectorFactory.class.getName());
    static final DotName OUTBOUND_CONNECTOR = DotName.createSimple(OutboundConnector.class.getName());

    static final DotName SMALLRYE_BLOCKING = DotName.createSimple(io.smallrye.common.annotation.Blocking.class.getName());

    static final DotName MESSAGE_CONVERTER = DotName.createSimple(MessageConverter.class.getName());
    static final DotName KEY_VALUE_EXTRACTOR = DotName.createSimple(KeyValueExtractor.class.getName());

    static final DotName KEYED = DotName.createSimple(Keyed.class.getName());
    public static final DotName KEYED_MULTI = DotName.createSimple(KeyedMulti.class.getName());

    // Do not directly reference the MetricDecorator (due to its direct references to MP Metrics, which may not be present)
    static final DotName METRIC_DECORATOR = DotName
            .createSimple("io.smallrye.reactive.messaging.providers.metrics.MetricDecorator");
    static final DotName MICROMETER_DECORATOR = DotName
            .createSimple("io.smallrye.reactive.messaging.providers.metrics.MicrometerDecorator");

    // Used to detect REST endpoints and JAX-RS provider
    public static final DotName JAXRS_PATH = DotName.createSimple("jakarta.ws.rs.Path");
    public static final DotName REST_CONTROLLER = DotName
            .createSimple("org.springframework.web.bind.annotation.RestController");
    public static final DotName JAXRS_PROVIDER = DotName.createSimple("jakarta.ws.rs.ext.Provider");

    static final DotName CONTINUATION = DotName.createSimple("kotlin.coroutines.Continuation");
    static final DotName KOTLIN_UNIT = DotName.createSimple("kotlin.Unit");
    static final DotName ABSTRACT_SUBSCRIBING_COROUTINE_INVOKER = DotName
            .createSimple("io.quarkus.smallrye.reactivemessaging.runtime.kotlin.AbstractSubscribingCoroutineInvoker");

    static final DotName TRANSACTIONAL = DotName.createSimple("jakarta.transaction.Transactional");
    static final DotName RUN_ON_VIRTUAL_THREAD = DotName.createSimple(RunOnVirtualThread.class.getName());

    private ReactiveMessagingDotNames() {
    }

}
