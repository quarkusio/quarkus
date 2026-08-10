package io.quarkus.smallrye.reactivemessaging.rabbitmq.runtime.produi;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;

import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.smallrye.common.annotation.NonBlocking;

/**
 * Read-only Prod UI view of the RabbitMQ reactive-messaging channels. For each
 * channel backed by the {@code smallrye-rabbitmq} connector it shows the
 * exchange / queue / routing-key binding, derived entirely from configuration.
 * It deliberately reads only binding-related keys - never host, username,
 * password or credentials-provider settings - so no secrets are exposed. The
 * Dev UI card only links to the dev-services management console, which is not
 * available in production, so this is a bespoke read-only view.
 */
@ApplicationScoped
public class RabbitMqProdUIService {

    private static final String CONNECTOR = "smallrye-rabbitmq";
    private static final String INCOMING_PREFIX = "mp.messaging.incoming.";
    private static final String OUTGOING_PREFIX = "mp.messaging.outgoing.";
    private static final String CONNECTOR_SUFFIX = ".connector";
    private static final String DEFAULT = "(default)";

    @Inject
    Config config;

    @NonBlocking
    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get a read-only overview of the RabbitMQ channels and their exchange/queue bindings")
    public List<RabbitChannelInfo> getChannels() {
        List<RabbitChannelInfo> result = new ArrayList<>();

        for (String propertyName : config.getPropertyNames()) {
            String direction;
            String prefix;
            if (propertyName.startsWith(INCOMING_PREFIX) && propertyName.endsWith(CONNECTOR_SUFFIX)) {
                direction = "incoming";
                prefix = INCOMING_PREFIX;
            } else if (propertyName.startsWith(OUTGOING_PREFIX) && propertyName.endsWith(CONNECTOR_SUFFIX)) {
                direction = "outgoing";
                prefix = OUTGOING_PREFIX;
            } else {
                continue;
            }

            if (!CONNECTOR.equals(config.getOptionalValue(propertyName, String.class).orElse(""))) {
                continue;
            }

            String channel = propertyName.substring(prefix.length(), propertyName.length() - CONNECTOR_SUFFIX.length());
            boolean incoming = "incoming".equals(direction);

            result.add(new RabbitChannelInfo(
                    channel,
                    direction,
                    firstConfigured(direction, channel, "exchange.name"),
                    firstConfigured(direction, channel, "exchange.type"),
                    incoming ? firstConfigured(direction, channel, "queue.name") : "-",
                    incoming ? firstConfigured(direction, channel, "routing-keys")
                            : firstConfigured(direction, channel, "default-routing-key")));
        }

        result.sort((a, b) -> a.channel().compareTo(b.channel()));
        return result;
    }

    private String firstConfigured(String direction, String channel, String key) {
        String property = "mp.messaging." + direction + "." + channel + "." + key;
        return config.getOptionalValue(property, String.class).orElse(DEFAULT);
    }

    public record RabbitChannelInfo(String channel, String direction, String exchange, String exchangeType,
            String queue, String routingKeys) {
    }
}
