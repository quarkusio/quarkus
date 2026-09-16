package io.quarkus.signals.runtime.metrics;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Shared constants and helpers used by the Micrometer metrics integration.
 * <p>
 * All meters are simple counters and are deliberately tagged only with code-bounded dimensions (the raw signal type, the
 * emission type and, for request-reply, the response type) so that the total number of time series stays bounded by the
 * application code and does not grow with traffic or with the number of receivers.
 */
public final class MetricsSupport {

    /**
     * Counter incremented once per emission (i.e., per {@code publish}/{@code send}/{@code request} call).
     */
    public static final String EMISSIONS = "signals.emissions";

    /**
     * Counter incremented once per completed receiver invocation. For a multicast {@code publish} with N matching
     * receivers it is incremented N times. The outcome is distinguished by the {@link #TAG_ERROR_TYPE} tag.
     */
    public static final String RECEIVER_EXECUTIONS = "signals.receiver.executions";

    /**
     * The raw signal type FQCN, e.g. {@code com.example.OrderPlaced}. The raw type is used (not the parameterized type
     * name) to keep the cardinality bounded.
     */
    public static final String TAG_SIGNAL_TYPE = "signal.type";

    /**
     * The emission type: {@code PUBLISH}, {@code SEND} or {@code REQUEST}.
     */
    public static final String TAG_EMISSION_TYPE = "emission.type";

    /**
     * The raw response type FQCN for a request-reply emission, or {@link #RESPONSE_TYPE_NONE} otherwise. A single meter
     * must always carry the same tag keys, hence the sentinel value.
     */
    public static final String TAG_RESPONSE_TYPE = "response.type";

    /**
     * The exception class name of a failed receiver invocation, or {@link #ERROR_TYPE_NONE} for a successful one. A
     * single meter must always carry the same tag keys, hence the sentinel value.
     */
    public static final String TAG_ERROR_TYPE = "error.type";

    /**
     * The value of the {@link #TAG_RESPONSE_TYPE} tag when the emission is not a request-reply.
     */
    public static final String RESPONSE_TYPE_NONE = "none";

    /**
     * The value of the {@link #TAG_ERROR_TYPE} tag when the receiver invocation completed successfully.
     */
    public static final String ERROR_TYPE_NONE = "none";

    static String rawTypeName(Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            return parameterizedType.getRawType().getTypeName();
        }
        return type.getTypeName();
    }

    private MetricsSupport() {
    }
}
