package io.quarkus.redis.datasource.stream;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

/**
 * Represents the extra argument of the <a href="https://redis.io/commands/xgroup-create/>XGROUP CREATE</a> command.
 */
public class XGroupCreateArgs implements RedisCommandExtraArguments {

    private boolean mkstream;
    private String entriesRead;

    /**
     * If a stream does not exist, you can create it automatically with length of 0.
     *
     * @return the current {@code XGroupCreateArgs}
     */
    public XGroupCreateArgs mkstream() {
        this.mkstream = true;
        return this;
    }

    /**
     * To enable consumer group lag tracking, specify the optional {@code entries_read} named argument with an arbitrary
     * ID. An arbitrary ID is any ID that isn't the ID of the stream's first entry, last entry, or zero ("0-0") ID. Use
     * it to find out how many entries are between the arbitrary ID (excluding it) and the stream's last entry. Set the
     * {@code entries_read} the stream's {@code entries_added} subtracted by the number of entries.
     * <p>
     * Requires REdis 7.0.0+
     *
     * @param id
     *        the arbitrary id
     *
     * @return the current {@code XGroupCreateArgs}
     */
    public XGroupCreateArgs entriesRead(String id) {
        this.entriesRead = id;
        return this;
    }

    @Override
    public List<Object> toArgs() {
        List<Object> args = new ArrayList<>();
        if (mkstream) {
            args.add("MKSTREAM");
        }
        if (entriesRead != null) {
            args.add("ENTRIESREAD");
            args.add(entriesRead);
        }

        return args;
    }
}
