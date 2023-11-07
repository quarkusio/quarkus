package io.quarkus.redis.datasource.geo;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

public class GeoAddArgs implements RedisCommandExtraArguments {

    private boolean nx = false;
    private boolean xx = false;
    private boolean ch = false;

    /**
     * Don't update already existing elements. Always add new elements.
     *
     * @return the current {@code GeoaddArgs}
     **/
    public GeoAddArgs nx() {
        this.nx = true;
        return this;
    }

    /**
     * Only update elements that already exist. Never add elements.
     *
     * @return the current {@code GeoaddArgs}
     **/
    public GeoAddArgs xx() {
        this.xx = true;
        return this;
    }

    /**
     * Modify the return value from the number of new elements added, to the total number of elements changed.
     * (CH is an abbreviation of changed).
     *
     * @return the current {@code GeoaddArgs}
     **/
    public GeoAddArgs ch() {
        this.ch = true;
        return this;
    }

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
        if (ch) {
            args.add("CH");
        }
        return args;
    }
}
