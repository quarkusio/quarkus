package io.quarkus.redis.datasource.stream;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

/**
 * Represents the extra argument of the <a href="https://redis.io/commands/xgroup-setid/>XGROUP SETID</a> command.
 */
public class XGroupSetIdArgs implements RedisCommandExtraArguments {

    private long entriesRead = -1;

    /**
     * To enable consumer group lag tracking, specify the optional {@code entries_read} named argument with an arbitrary
     * ID. An arbitrary ID is any ID that isn't the ID of the stream's first entry, last entry, or zero ("0-0") ID.
     * Use it to find out how many entries are between the arbitrary ID (excluding it) and the stream's last entry.
     * Set the {@code entries_read} the stream's {@code entries_added} subtracted by the number of entries.
     * <p>
     * Requires REdis 7.0.0+
     *
     * @param id the arbitrary id
     * @return the current {@code XGroupCreateArgs}
     */
    public XGroupSetIdArgs entriesRead(long id) {
        this.entriesRead = id;
        return this;
    }

    @Override
    public List<Object> toArgs() {
        List<Object> args = new ArrayList<>();
        if (entriesRead > 0) {
            args.add("ENTRIESREAD");
            args.add(Long.toString(entriesRead));
        }

        return args;
    }
}
