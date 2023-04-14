package io.quarkus.observability.promql.client.rest;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Stream;

import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

/**
 * A {@link ParamConverterProvider} for some common types. To register, subclass and
 * annotate with {@link Provider} and possibly {@link ConstrainedTo} annotations.
 *
 * @see InstantFormat
 */
public abstract class InstantParamConverterProvider extends AbstractParamConverterProvider {
    protected final Logger log = Logger.getLogger(getClass());

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (log.isDebugEnabled()) {
            log.debugf("getConverter(rawType=%s, annotations=%s)", rawType.getName(), Arrays.toString(annotations));
        }

        if (Instant.class.isAssignableFrom(rawType)) {
            var instantFormatKind = Stream
                    .of(annotations)
                    .filter(InstantFormat.class::isInstance)
                    .map(InstantFormat.class::cast)
                    .findFirst()
                    .map(InstantFormat::value)
                    .orElse(InstantFormat.Kind.ISO);

            return cast(rawType, new PC<>(instantFormatKind.fromString, instantFormatKind.toString));
        }
        return null;
    }
}
