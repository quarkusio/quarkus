package io.quarkus.resteasy.reactive.data.hibernate.runtime;

import java.util.Locale;

import jakarta.data.Direction;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;

/**
 * Extracts a {@link Direction} from an HTTP query parameter.
 * <ul>
 * <li>Query parameter — sort direction, either {@code ASC} or {@code DESC}, case-insensitive
 * (default: {@code null})</li>
 * </ul>
 * Example: {@code ?direction=desc} produces {@code Direction.DESC}.
 */
public class DirectionParamExtractor implements ParameterExtractor {

    public final String parameterName;

    public DirectionParamExtractor(String parameterName) {
        this.parameterName = parameterName;
    }

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        String value = (String) context.getQueryParameter(parameterName, true, false);
        if (value == null) {
            return null;
        }
        return Direction.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
