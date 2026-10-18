package io.quarkus.redis.datasource.json;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

public class JsonSetArgs implements RedisCommandExtraArguments {

    private boolean nx = false;
    private boolean xx = false;

    /**
     * Don't update already existing elements. Always add new elements.
     *
     * @return the current {@code GeoaddArgs}
     **/
    public JsonSetArgs nx() {
        this.nx = true;
        return this;
    }

    /**
     * Only update elements that already exist. Never add elements.
     *
     * @return the current {@code GeoaddArgs}
     **/
    public JsonSetArgs xx() {
        this.xx = true;
        return this;
    }

    @Override
    public List<Object> toArgs() {
        if (xx && nx) {
            throw new IllegalArgumentException("Cannot set XX and NX together");
        }
        List<Object> args = new ArrayList<>();
        if (xx) {
            args.add("XX");
        }
        if (nx) {
            args.add("NX");
        }
        return args;
    }
}
