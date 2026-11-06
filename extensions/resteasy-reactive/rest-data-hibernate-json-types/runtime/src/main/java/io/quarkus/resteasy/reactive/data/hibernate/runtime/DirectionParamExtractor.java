package io.quarkus.resteasy.reactive.data.hibernate.runtime;

import java.util.Locale;

import jakarta.data.Direction;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;

/**
 * Extracts a {@link Direction} from an HTTP query parameter.
 * <p>
 * <b>Parameter naming:</b>
 * <ul>
 * <li><b>Default name:</b> {@code "direction"} when not annotated or using bare {@code @RestQuery}</li>
 * <li><b>Custom name:</b> Uses the value specified in {@code @RestQuery("customName")}</li>
 * <li><b>Inferred name:</b> Uses the Java parameter name when {@code @RestQuery} or
 * {@code @RestQuery("")} is used</li>
 * </ul>
 * <p>
 * Query parameter value: {@code ASC} or {@code DESC}, case-insensitive (default: {@code null})
 * <p>
 * <b>Examples:</b>
 *
 * <pre>
 * {@code Direction direction}  →  ?direction=DESC
 * {@code @RestQuery("sortOrder") Direction sortOrder}  →  ?sortOrder=ASC
 * {@code @RestQuery Direction sortDir}  →  ?sortDir=DESC
 * </pre>
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
