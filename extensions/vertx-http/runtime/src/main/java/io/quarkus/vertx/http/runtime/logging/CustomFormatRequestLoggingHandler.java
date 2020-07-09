package io.quarkus.vertx.http.runtime.logging;

import java.text.MessageFormat;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class CustomFormatRequestLoggingHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = Logger.getLogger(CustomFormatRequestLoggingHandler.class.getName());

    private final RequestLogMessageFormatter requestLogMessageFormatter;

    public CustomFormatRequestLoggingHandler(RequestLogMessageFormatter requestLogMessageFormatter) {
        this.requestLogMessageFormatter = requestLogMessageFormatter;
    }

    @Override
    public void handle(RoutingContext event) {
        final RequestLogMessage requestLogMessage = RequestLogMessage.builder()
                .headers(
                        event.request()
                                .headers()
                                .entries()
                                .stream()
                                .collect(Collectors
                                        .toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .method(event.request().method().name())
                .path(event.request().path())
                .version(event.request().version())
                .build();

        LOGGER.info(MessageFormat.format("Request{0}{1}", System.lineSeparator(),
                this.requestLogMessageFormatter.format(requestLogMessage)));

        event.next();
    }

}
