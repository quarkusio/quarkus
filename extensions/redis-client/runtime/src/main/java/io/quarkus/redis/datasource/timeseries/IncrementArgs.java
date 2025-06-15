package io.quarkus.redis.datasource.timeseries;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

/**
 * Represents the extra arguments of the {@code ts.decrby} and {@code tx.incrby} commands.
 */
public class IncrementArgs implements RedisCommandExtraArguments {

    private long timestamp = -1;

    private Duration retention;

    private boolean uncompressed;

    private int chunkSize;
    private final Map<String, Object> labels = new HashMap<>();

    /**
     * Set (integer) UNIX sample timestamp in milliseconds.
     * <p>
     * timestamp must be equal to or higher than the maximum existing timestamp. When equal, the value of the sample
     * with the maximum existing timestamp is decreased. If it is higher, a new sample with a timestamp set to timestamp
     * is created, and its value is set to the value of the sample with the maximum existing timestamp minus value.
     * <p>
     * If the time series is empty, the value is set to {@code value}.
     * <p>
     * When not specified, the timestamp is set according to the server clock.
     *
     * @param value
     *        the value
     *
     * @return the current {@code IncrementArgs}
     */
    public IncrementArgs setTimestamp(long value) {
        this.timestamp = value;
        return this;
    }

    /**
     * Set the maximum retention period, compared to the maximum existing timestamp, in milliseconds.
     * <p>
     * Use it only if you are creating a new time series. It is ignored if you are adding samples to an existing time
     * series.
     *
     * @param retention
     *        the retention, must not be {@code null}
     *
     * @return the current {@code IncrementArgs}
     */
    public IncrementArgs setRetention(Duration retention) {
        this.retention = nonNull(retention, "retention");
        return this;
    }

    /**
     * Changes data storage from compressed (default) to uncompressed.
     * <p>
     * Use it only if you are creating a new time series. It is ignored if you are adding samples to an existing time
     * series
     *
     * @return the current {@code IncrementArgs}
     */
    public IncrementArgs uncompressed() {
        this.uncompressed = true;
        return this;
    }

    /**
     * Sets memory size, in bytes, allocated for each data chunk. Use it only if you are creating a new time series. It
     * is ignored if you are adding samples to an existing time series.
     *
     * @param size
     *        the chunk size, between 48 and 1048576
     *
     * @return the current {@code IncrementArgs}
     */
    public IncrementArgs chunkSize(int size) {
        this.chunkSize = size;
        return this;
    }

    /**
     * Set a label-value pairs that represent metadata labels of the time series.
     *
     * @param label
     *        the label, must not be {@code null}
     * @param value
     *        the value, must not be {@code  null}
     *
     * @return the current {@code IncrementArgs}
     */
    public IncrementArgs label(String label, Object value) {
        labels.put(label, value);
        return this;
    }

    @Override
    public List<Object> toArgs() {
        List<Object> list = new ArrayList<>();
        if (timestamp >= 0) {
            list.add("TIMESTAMP");
            list.add(Long.toString(timestamp));
        }
        if (retention != null) {
            list.add("RETENTION");
            if (retention == Duration.ZERO) {
                list.add("0");
            } else {
                list.add(Long.toString(retention.toMillis()));
            }
        }

        if (uncompressed) {
            list.add("UNCOMPRESSED");
        }

        if (chunkSize > 0) {
            list.add("CHUNK_SIZE");
            list.add(Integer.toString(chunkSize));
        }

        if (!labels.isEmpty()) {
            list.add("LABELS");
            for (Map.Entry<String, Object> entry : labels.entrySet()) {
                list.add(entry.getKey());
                list.add(entry.getValue().toString());
            }
        }

        return list;
    }
}
