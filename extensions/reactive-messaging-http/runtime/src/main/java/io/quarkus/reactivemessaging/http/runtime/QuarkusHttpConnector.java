package io.quarkus.reactivemessaging.http.runtime;

import static io.smallrye.reactive.messaging.annotations.ConnectorAttribute.Direction.INCOMING;
import static io.smallrye.reactive.messaging.annotations.ConnectorAttribute.Direction.INCOMING_AND_OUTGOING;
import static io.smallrye.reactive.messaging.annotations.ConnectorAttribute.Direction.OUTGOING;

import java.time.Duration;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.messaging.spi.OutgoingConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.jboss.logging.Logger;

import io.quarkus.reactivemessaging.http.runtime.serializers.SerializerFactoryBase;
import io.quarkus.runtime.configuration.DurationConverter;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.annotations.ConnectorAttribute;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;

/**
 * Quarkus-specific reactive messaging connector for HTTP
 */
@ConnectorAttribute(name = "url", type = "string", direction = OUTGOING, description = "The target URL", mandatory = true)
@ConnectorAttribute(name = "serializer", type = "string", direction = OUTGOING, description = "Message serializer")
@ConnectorAttribute(name = "maxPoolSize", type = "int", direction = OUTGOING, description = "Maximum pool size for connections")
@ConnectorAttribute(name = "maxWaitQueueSize", type = "int", direction = OUTGOING, description = "Maximum requests allowed in the wait queue of the underlying client.  If the value is set to a negative number then the queue will be unbounded")
@ConnectorAttribute(name = "maxRetries", type = "int", direction = OUTGOING, description = "The number of attempts to make for sending a request to a remote endpoint. Must not be less than zero", defaultValue = QuarkusHttpConnector.DEFAULT_MAX_ATTEMPTS_STR)
@ConnectorAttribute(name = "jitter", type = "string", direction = OUTGOING, description = "Configures the random factor when using back-off with maxRetries > 0", defaultValue = QuarkusHttpConnector.DEFAULT_JITTER)
@ConnectorAttribute(name = "delay", type = "string", direction = OUTGOING, description = "Configures a back-off delay between attempts to send a request. A random factor (jitter) is applied to increase the delay when several failures happen.")

@ConnectorAttribute(name = "method", type = "string", direction = INCOMING_AND_OUTGOING, description = "The HTTP method (either `POST` or `PUT`)", defaultValue = "POST")
@ConnectorAttribute(name = "path", type = "string", direction = INCOMING, description = "The path of the endpoint", mandatory = true)
@ConnectorAttribute(name = "buffer-size", type = "string", direction = INCOMING, description = "HTTP endpoint buffers messages if a consumer is not able to keep up. This setting specifies the size of the buffer.", defaultValue = QuarkusHttpConnector.DEFAULT_SOURCE_BUFFER_STR)

@Connector(QuarkusHttpConnector.NAME)
@ApplicationScoped
public class QuarkusHttpConnector implements IncomingConnectorFactory, OutgoingConnectorFactory {
    private static final Logger log = Logger.getLogger(QuarkusHttpConnector.class);

    static final String DEFAULT_JITTER = "0.5";
    static final String DEFAULT_MAX_ATTEMPTS_STR = "0";

    static final String DEFAULT_SOURCE_BUFFER_STR = "8";

    public static final Integer DEFAULT_SOURCE_BUFFER = Integer.valueOf(DEFAULT_SOURCE_BUFFER_STR);

    public static final String NAME = "quarkus-http";

    @Inject
    ReactiveHttpHandlerBean handlerBean;

    @Inject
    Vertx vertx;

    @Inject
    SerializerFactoryBase serializerFactory;

    @Override
    public PublisherBuilder<HttpMessage<?>> getPublisherBuilder(Config configuration) {
        QuarkusHttpConnectorIncomingConfiguration config = new QuarkusHttpConnectorIncomingConfiguration(configuration);
        String methodAsString = config.getMethod();
        HttpMethod method = getMethod(methodAsString);

        Multi<HttpMessage<?>> processor = handlerBean.getProcessor(config.getPath(), method);

        return ReactiveStreams.fromPublisher(processor);
    }

    private HttpMethod getMethod(String methodAsString) {
        try {
            return HttpMethod.valueOf(methodAsString);
        } catch (IllegalArgumentException e) {
            String error = "Unsupported HTTP method: " + methodAsString + ". The supported methods are: "
                    + HttpMethod.values();
            log.warn(error, e);
            throw new IllegalArgumentException(error);
        }
    }

    @Override
    public SubscriberBuilder<? extends Message<?>, Void> getSubscriberBuilder(Config configuration) {
        QuarkusHttpConnectorOutgoingConfiguration config = new QuarkusHttpConnectorOutgoingConfiguration(configuration);
        String url = config.getUrl();
        String method = getMethod(config.getMethod()).name();
        String serializer = config.getSerializer().orElse(null);
        Optional<String> maybeDelay = config.getDelay();

        Optional<Duration> delay = maybeDelay.map(DurationConverter::parseDuration);

        String jitterAsString = config.getJitter();
        Integer maxRetries = config.getMaxRetries();

        Optional<Integer> maxPoolSize = config.getMaxPoolSize();
        Optional<Integer> maxWaitQueueSize = config.getMaxWaitQueueSize();

        double jitter;
        try {
            jitter = Double.valueOf(jitterAsString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("Failed to parse jitter value '%s' to a double.", jitterAsString));
        }

        return new HttpSink(vertx, method, url, serializer, maxRetries, jitter, delay, maxPoolSize, maxWaitQueueSize,
                serializerFactory).sink();
    }

}
