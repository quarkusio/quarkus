package io.quarkus.redis.datasource.timeseries;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

/**
 * Represents the extra arguments of the {@code ts.alter} command.
 */
public class AlterArgs implements RedisCommandExtraArguments {

    private Duration retention;
    private String enc;
    private int chunkSize;
    private DuplicatePolicy policy;
    private final Map<String, Object> labels = new HashMap<>();

    /**
     * Set the maximum age for samples compared to the highest reported timestamp, in milliseconds.
     * Samples are expired based solely on the difference between their timestamp and the timestamps passed to
     * subsequent TS.ADD, TS.MADD, TS.INCRBY, and TS.DECRBY calls.
     * <p>
     * When set to 0, samples never expire. When not specified, the option is set to the global RETENTION_POLICY
     * configuration of the database, which by default is 0.
     *
     * @param retention the retention, must not be {@code null}
     * @return the current {@code AlterArgs}
     */
    public AlterArgs setRetention(Duration retention) {
        this.retention = nonNull(retention, "retention");
        return this;
    }

    /**
     * Set the retention duration so the samples never expire.
     *
     * @return the current {@code AlterArgs}
     * @see #setRetention(Duration)
     */
    public AlterArgs forever() {
        this.retention = Duration.ZERO;
        return this;
    }

    /**
     * Sets the initial allocation size, in bytes, for the data part of each new chunk.
     * Actual chunks may consume more memory. Changing chunkSize (using TS.ALTER) does not affect existing chunks.
     * <p>
     * Must be a multiple of 8 in the range [48 .. 1048576].
     * When not specified, it is set to 4096 bytes (a single memory page).
     *
     * @param size the chunk size, between 48 and 1048576
     * @return the current {@code AlterArgs}
     */
    public AlterArgs chunkSize(int size) {
        this.chunkSize = size;
        return this;
    }

    /**
     * Set the policy for handling insertion (TS.ADD and TS.MADD) of multiple samples with identical timestamps.
     * <p>
     * When not specified: set to the global DUPLICATE_POLICY configuration of the database (which, by default, is BLOCK).
     *
     * @param policy the policy, must not be {@code null}
     * @return the current {@code AlterArgs}
     */
    public AlterArgs duplicatePolicy(DuplicatePolicy policy) {
        this.policy = policy;
        return this;
    }

    /**
     * Set a label-value pairs that represent metadata labels of the key and serve as a secondary index.
     * <p>
     * The TS.MGET, TS.MRANGE, and TS.MREVRANGE commands operate on multiple time series based on their labels.
     * The TS.QUERYINDEX command returns all time series keys matching a given filter based on their labels.
     *
     * @param label the label, must not be {@code null}
     * @param value the value, must not be {@code  null}
     * @return the current {@code AlterArgs}
     */
    public AlterArgs label(String label, Object value) {
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

        if (policy != null) {
            list.add("DUPLICATE_POLICY");
            list.add(policy.name().toUpperCase());
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
