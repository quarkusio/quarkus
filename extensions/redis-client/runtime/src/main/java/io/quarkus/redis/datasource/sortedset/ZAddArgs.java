package io.quarkus.redis.datasource.sortedset;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

public class ZAddArgs implements RedisCommandExtraArguments {

    private boolean nx = false;
    private boolean xx = false;
    private boolean ch = false;
    private boolean lt = false;
    private boolean gt = false;

    /**
     * Only add new elements. Don't update already existing elements.
     *
     * @return the current {@code ZAddArgs}
     **/
    public ZAddArgs nx() {
        this.nx = true;
        return this;
    }

    /**
     * Only update elements that already exist. Don't add new elements.
     *
     * @return the current {@code ZAddArgs}
     **/
    public ZAddArgs xx() {
        this.xx = true;
        return this;
    }

    /**
     * Modify the return value from the number of new elements added, to the total number of elements changed (CH is an
     * abbreviation of changed). Changed elements are new elements added and elements already existing for which the
     * score was updated. So elements specified in the command line having the same score as they had in the past are
     * not counted.
     *
     * @return the current {@code ZAddArgs}
     **/
    public ZAddArgs ch() {
        this.ch = true;
        return this;
    }

    /**
     * Only update existing elements if the new score is less than the current score. This flag doesn't prevent adding
     * new elements.
     *
     * @return the current {@code ZAddArgs}
     **/
    public ZAddArgs lt() {
        this.lt = true;
        return this;
    }

    /**
     * Only update existing elements if the new score is greater than the current score. This flag doesn't prevent
     * adding new elements.
     *
     * @return the current {@code ZAddArgs}
     **/
    public ZAddArgs gt() {
        this.gt = true;
        return this;
    }

    @Override
    public List<Object> toArgs() {
        if (xx && nx) {
            throw new IllegalArgumentException("Cannot use XX and NX together");
        }
        if (lt && gt) {
            throw new IllegalArgumentException("Cannot use LT and GT together");
        }

        List<Object> args = new ArrayList<>();
        putFlag(args, nx, "NX");
        putFlag(args, xx, "XX");
        putFlag(args, lt, "LT");
        putFlag(args, gt, "GT");
        putFlag(args, ch, "CH");
        return args;
    }

    public void putFlag(List<Object> args, boolean value, String flag) {
        if (value) {
            args.add(flag);
        }
    }
}
