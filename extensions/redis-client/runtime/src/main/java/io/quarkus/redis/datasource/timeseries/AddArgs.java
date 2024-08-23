package io.quarkus.redis.datasource.timeseries;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

/**
 * Represents the extra arguments of the {@code ts.add} command.
 */
public class AddArgs implements RedisCommandExtraArguments {

    private Duration retention;
    private String enc;
    private int chunkSize;
    private DuplicatePolicy onDuplicate;
    private final Map<String, Object> labels = new HashMap<>();

    /**
     * Set the maximum retention period, compared to the maximum existing timestamp, in milliseconds.
     * <p>
     * Use it only if you are creating a new time series. It is ignored if you are adding samples to an existing time
     * series.
     *
     * @param retention the retention, must not be {@code null}
     * @return the current {@code AddArgs}
     */
    public AddArgs setRetention(Duration retention) {
        this.retention = nonNull(retention, "retention");
        return this;
    }

    /**
     * Set the series sample's encoding format to {@code COMPRESSED}
     * Use it only if you are creating a new time series. It is ignored if you are adding samples to an existing
     * time series.
     *
     * @return the current {@code AddArgs}
     */
    public AddArgs compressed() {
        this.enc = "COMPRESSED";
        return this;
    }

    /**
     * Set the series sample's encoding format to {@code UNCOMPRESSED}
     * Use it only if you are creating a new time series. It is ignored if you are adding samples to an existing
     * time series.
     *
     * @return the current {@code AddArgs}
     */
    public AddArgs uncompressed() {
        this.enc = "UNCOMPRESSED";
        return this;
    }

    /**
     * Sets the memory size, in bytes, allocated for each data chunk.
     * Use it only if you are creating a new time series. It is ignored if you are adding samples to an existing
     * time series.
     *
     * @param size the chunk size, between 48 and 1048576
     * @return the current {@code AddArgs}
     */
    public AddArgs chunkSize(int size) {
        this.chunkSize = size;
        return this;
    }

    /**
     * Overwrite key and database configuration for DUPLICATE_POLICY, the policy for handling samples with identical
     * timestamps.
     *
     * @param policy the policy, must not be {@code null}
     * @return the current {@code AddArgs}
     */
    public AddArgs onDuplicate(DuplicatePolicy policy) {
        this.onDuplicate = policy;
        return this;
    }

    /**
     * Set a label-value pairs that represent metadata labels of the time series.
     *
     * @param label the label, must not be {@code null}
     * @param value the value, must not be {@code  null}
     * @return the current {@code AddArgs}
     */
    public AddArgs label(String label, Object value) {
        labels.put(label, value);
        return this;
    }

    @Override
    public List<Object> toArgs() {
        List<Object> list = new ArrayList<>();
        if (retention != null) {
            list.add("RETENTION");
            if (retention == Duration.ZERO) {
                list.add("0");
            } else {
                list.add(Long.toString(retention.toMillis()));
            }
        }

        if (enc != null) {
            list.add("ENCODING");
            list.add(enc);
        }

        if (chunkSize > 0) {
            list.add("CHUNK_SIZE");
            list.add(Integer.toString(chunkSize));
        }

        if (onDuplicate != null) {
            list.add("ON_DUPLICATE");
            list.add(onDuplicate.name().toUpperCase());
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
