package io.quarkus.redis.datasource.stream;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

public class XPendingArgs implements RedisCommandExtraArguments {

    private String owner;

    private Duration idle;

    /**
     * Sets the specific owner of the message
     *
     * @param owner the name of the consumer
     * @return the current {@code XPendingArgs}
     */
    public XPendingArgs consumer(String owner) {
        this.owner = owner;
        return this;
    }

    /**
     * Filters pending stream entries by their idle-time.
     *
     * @param idle the duration
     * @return the current {@code XPendingArgs}
     */
    public XPendingArgs idle(Duration idle) {
        this.idle = idle;
        return this;
    }

    public Duration idle() {
        return idle;
    }

    @Override
    public List<Object> toArgs() {
        List<Object> args = new ArrayList<>();

        if (owner != null) {
            args.add(owner);
        }

        return args;
    }
}
