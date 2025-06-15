package io.quarkus.redis.datasource.keys;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

/**
 * Argument for the Redis <a href="https://redis.io/commands/expire">EXPIRE</a> and
 * <a href="https://redis.io/commands/expireat">EXPIREAT</a> commands.
 */
public class ExpireArgs implements RedisCommandExtraArguments {
    private boolean xx;
    private boolean lt;
    private boolean nx;
    private boolean gt;

    /**
     * Sets the expiry only when the key has an existing expiry
     *
     * @return the current {@code ExpireArgs}
     **/
    public ExpireArgs xx() {
        this.xx = true;
        return this;
    }

    /**
     * Sets the expiry only when the new expiry is less than current one
     *
     * @return the current {@code ExpireArgs}
     **/
    public ExpireArgs lt() {
        this.lt = true;
        return this;
    }

    /**
     * Sets the expiry only when the key has no expiry.
     *
     * @return the current {@code ExpireArgs}
     **/
    public ExpireArgs nx() {
        this.nx = true;
        return this;
    }

    /**
     * Set the expiry only when the new expiry is greater than current one
     *
     * @return the current {@code ExpireArgs}
     **/
    public ExpireArgs gt() {
        this.gt = true;
        return this;
    }

    @Override
    public List<Object> toArgs() {
        List<Object> args = new ArrayList<>();

        boolean exclusion = false;
        if (nx) {
            args.add("NX");
            exclusion = true;
        }
        if (xx) {
            args.add("XX");
        }
        if (lt) {
            if (exclusion) {
                throw new IllegalArgumentException("Only one value from `GT`, `LT` and `NX` can be set");
            }
            exclusion = true;
            args.add("LT");
        }
        if (gt) {
            if (exclusion) {
                throw new IllegalArgumentException("Only one value from `GT`, `LT` and `NX` can be set");
            }
            args.add("GT");
        }
        return args;
    }
}
