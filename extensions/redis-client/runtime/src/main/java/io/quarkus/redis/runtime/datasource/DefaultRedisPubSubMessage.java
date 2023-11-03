package io.quarkus.redis.runtime.datasource;

import io.quarkus.redis.datasource.pubsub.RedisPubSubMessage;

public class DefaultRedisPubSubMessage<V> implements RedisPubSubMessage<V> {

    private final V payload;
    private final String channel;

    public DefaultRedisPubSubMessage(V payload, String channel) {
        this.payload = payload;
        this.channel = channel;
    }

    @Override
    public V getPayload() {
        return payload;
    }

    @Override
    public String getChannel() {
        return channel;
    }
}
