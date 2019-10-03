package io.quarkus.reactivemessaging.http.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

import io.reactivex.processors.BehaviorProcessor;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 28/08/2019
 */
@Connector(QuarkusWebsocketConnector.NAME)
@ApplicationScoped
public class QuarkusWebsocketConnector implements IncomingConnectorFactory {
    public static final String NAME = "quarkus-websocket";

    @Inject
    ReactiveHttpHandlerBean handlerBean;

    @Override
    public PublisherBuilder<HttpMessage> getPublisherBuilder(Config config) {
        String path = config.getOptionalValue("path", String.class)
                .orElseThrow(() -> new IllegalArgumentException("The `path` must be set"));

        BehaviorProcessor<HttpMessage> processor = handlerBean.getWebsocketProcessor(path);
        return ReactiveStreams.fromPublisher(processor);
    }
}
