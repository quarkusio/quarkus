package io.quarkus.resteasy.reactive.data.hibernate.runtime;

import java.util.Locale;

import jakarta.data.Direction;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;

/**
 * Extracts a {@link Direction} from the HTTP query parameter {@code direction}.
 * <ul>
 * <li>{@code direction} — sort direction, either {@code ASC} or {@code DESC}, case-insensitive
 * (default: {@code null})</li>
 * </ul>
 * Example: {@code ?direction=desc} produces {@code Direction.DESC}.
 */
public class DirectionParamExtractor implements ParameterExtractor {

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        String value = (String) context.getQueryParameter("direction", true, false);
        if (value == null) {
            return null;
        }
        return Direction.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
