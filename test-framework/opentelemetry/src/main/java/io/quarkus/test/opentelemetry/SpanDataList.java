package io.quarkus.test.opentelemetry;

import java.util.List;
import java.util.function.Predicate;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.data.SpanData;

/**
 * Spans finished by the application under test, as fetched by a {@link SpanExporterClient}.
 * <p>
 * It is an unmodifiable list of the {@link SpanData} of OpenTelemetry, with the lookups that tests keep needing. The
 * filtering methods return a new instance and leave this one untouched.
 * <p>
 * Being a list, it works with the assertions of AssertJ and of OpenTelemetry:
 *
 * <pre>{@code
 * assertThat(spans).hasSize(3);
 * // the spans grouped by trace: there is exactly one trace
 * TracesAssert.assertThat(spans).hasSize(1);
 * }</pre>
 */
public interface SpanDataList extends List<SpanData> {

    SpanDataList ofKind(SpanKind kind);

    SpanDataList named(String name);

    /**
     * @return the spans that have no parent
     */
    SpanDataList roots();

    SpanDataList childrenOf(SpanData parent);

    SpanDataList filter(Predicate<SpanData> predicate);

    /**
     * @return the only span
     * @throws AssertionError if there is not exactly one span
     */
    SpanData single();
}
