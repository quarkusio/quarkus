package io.quarkus.resteasy.reactive.data.hibernate.runtime;

import jakarta.data.Limit;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;

/**
 * Extracts a {@link Limit} from HTTP query parameters.
 * <p>
 * Supports two modes:
 * <ul>
 * <li><b>Count mode:</b> {@code ?limit=N} optionally with {@code &startAt=M} —
 * produces {@code Limit.of(N)} or {@code Limit.range(M, M+N-1)}</li>
 * <li><b>Range mode:</b> {@code ?startAt=M&endAt=N} —
 * produces {@code Limit.range(M, N)}</li>
 * </ul>
 * <p>
 * Defaults: {@code startAt=1}. Returns {@code null} when neither {@code limit}
 * nor {@code endAt} is provided.
 */
public class LimitParamExtractor implements ParameterExtractor {

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        String limitStr = (String) context.getQueryParameter("limit", true, false);
        String startAtStr = (String) context.getQueryParameter("startAt", true, false);
        String endAtStr = (String) context.getQueryParameter("endAt", true, false);

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
