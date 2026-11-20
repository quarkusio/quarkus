package io.quarkus.resteasy.reactive.data.hibernate.runtime;

import jakarta.data.Limit;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;

/**
 * Extracts a {@link Limit} from HTTP query parameters.
 * <p>
 * <b>Parameter naming:</b>
 * <ul>
 * <li><b>Default name:</b> When the parameter is not annotated with {@code @RestQuery},
 * or annotated without a value ({@code @RestQuery}), or with an empty value
 * ({@code @RestQuery("")}), the parameter name defaults to {@code "limit"}.</li>
 * <li><b>Custom name:</b> When annotated with {@code @RestQuery("customName")},
 * uses {@code "customName"} as the parameter name.</li>
 * </ul>
 * <p>
 * <b>Extraction modes:</b>
 * <ul>
 * <li><b>Count mode:</b> {@code ?<name>=N} optionally with {@code &<name>_startAt=M} —
 * produces {@code Limit.of(N)} or {@code Limit.range(M, M+N-1)}</li>
 * <li><b>Range mode:</b> {@code ?<name>_startAt=M&<name>_endAt=N} —
 * produces {@code Limit.range(M, N)}</li>
 * </ul>
 * <p>
 * <b>Auxiliary parameters:</b>
 * <ul>
 * <li><b>Default name:</b> Uses unprefixed {@code startAt} and {@code endAt}</li>
 * <li><b>Custom name:</b> Uses prefixed {@code <name>_startAt} and {@code <name>_endAt}</li>
 * </ul>
 * This allows multiple {@code Limit} parameters with different names to coexist
 * without interfering with each other.
 * <p>
 * <b>Defaults:</b> {@code startAt=1}. Returns {@code null} when neither the main
 * parameter nor {@code endAt} is provided.
 * <p>
 * <b>Examples:</b>
 *
 * <pre>
 * // Default name:
 * {@code Limit limit}  →  ?limit=10&startAt=5
 *
 * // Custom name:
 * {@code @RestQuery("maxRows") Limit maxRows}  →  ?maxRows=10&maxRows_startAt=5
 *
 * // Multiple limits:
 * {@code @RestQuery("maxRows") Limit maxRows, @RestQuery("recordLimit") Limit recordLimit}
 *   →  ?maxRows=10&maxRows_startAt=5&recordLimit=20&recordLimit_startAt=15
 * </pre>
 */
public class LimitParamExtractor implements ParameterExtractor {

    public final String parameterName;

    public LimitParamExtractor(String parameterName) {
        this.parameterName = parameterName;
    }

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        String limitStr = (String) context.getQueryParameter(parameterName, true, false);

        // Use prefixed auxiliary parameters for custom names, unprefixed for default "limit"
        // Default: "startAt", "endAt" (for backward compatibility)
        // Custom: "maxRows_startAt", "maxRows_endAt" when parameterName is "maxRows"
        String startAtParam = parameterName.equals("limit") ? "startAt" : parameterName + "_startAt";
        String endAtParam = parameterName.equals("limit") ? "endAt" : parameterName + "_endAt";

        String startAtStr = (String) context.getQueryParameter(startAtParam, true, false);
        String endAtStr = (String) context.getQueryParameter(endAtParam, true, false);

        if (limitStr == null && endAtStr == null) {
            return null;
        }

        if (endAtStr != null) {
            long startAt = startAtStr != null ? Long.parseLong(startAtStr) : 1;
            long endAt = Long.parseLong(endAtStr);
            return Limit.range(startAt, endAt);
        }

        int limit = Integer.parseInt(limitStr);
        if (startAtStr != null) {
            long startAt = Long.parseLong(startAtStr);
            return Limit.range(startAt, startAt + limit - 1);
        }
        return Limit.of(limit);
    }
}
