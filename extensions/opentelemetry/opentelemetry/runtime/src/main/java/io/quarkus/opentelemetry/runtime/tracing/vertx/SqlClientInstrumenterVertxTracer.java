package io.quarkus.opentelemetry.runtime.tracing.vertx;

import static io.quarkus.opentelemetry.runtime.OpenTelemetryConfig.INSTRUMENTATION_NAME;

import java.util.Map;
import java.util.function.BiConsumer;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.db.DbSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.db.SqlAttributesExtractor;
import io.vertx.core.Context;
import io.vertx.core.spi.tracing.SpanKind;
import io.vertx.core.spi.tracing.TagExtractor;
import io.vertx.core.tracing.TracingPolicy;

public class SqlClientInstrumenterVertxTracer implements
        InstrumenterVertxTracer<SqlClientInstrumenterVertxTracer.QueryTrace, SqlClientInstrumenterVertxTracer.QueryTrace> {
    private final Instrumenter<QueryTrace, QueryTrace> sqlClientInstrumenter;

    public SqlClientInstrumenterVertxTracer(final OpenTelemetry openTelemetry) {
        SqlClientAttributesExtractor sqlClientAttributesExtractor = new SqlClientAttributesExtractor();

        InstrumenterBuilder<QueryTrace, QueryTrace> serverBuilder = Instrumenter.builder(
                openTelemetry,
                INSTRUMENTATION_NAME, DbSpanNameExtractor.create(sqlClientAttributesExtractor));

        this.sqlClientInstrumenter = serverBuilder
                .addAttributesExtractor(sqlClientAttributesExtractor)
                .newClientInstrumenter((queryTrace, key, value) -> {
                });
    }

    @Override
    public <R> boolean canHandle(final R request, final TagExtractor<R> tagExtractor) {
        if (request instanceof QueryTrace) {
            return true;
        }

        return tagExtractor.extract(request).containsKey("db.statement");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> OpenTelemetryVertxTracer.SpanOperation sendRequest(
            final Context context,
            final SpanKind kind,
            final TracingPolicy policy,
            final R request,
            final String operation,
            final BiConsumer<String, String> headers,
            final TagExtractor<R> tagExtractor) {

        R queryTrace = (R) QueryTrace.queryTrace(tagExtractor.extract(request));
        return InstrumenterVertxTracer.super.sendRequest(context, kind, policy, queryTrace, operation, headers, tagExtractor);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> void receiveResponse(
            final Context context,
            final R response,
            final OpenTelemetryVertxTracer.SpanOperation spanOperation,
            final Throwable failure,
            final TagExtractor<R> tagExtractor) {

        InstrumenterVertxTracer.super.receiveResponse(context, response, spanOperation, failure, tagExtractor);
    }

    @Override
    public Instrumenter<QueryTrace, QueryTrace> getReceiveRequestInstrumenter() {
        return null;
    }

    @Override
    public Instrumenter<QueryTrace, QueryTrace> getSendResponseInstrumenter() {
        return null;
    }

    @Override
    public Instrumenter<QueryTrace, QueryTrace> getSendRequestInstrumenter() {
        return sqlClientInstrumenter;
    }

    @Override
    public Instrumenter<QueryTrace, QueryTrace> getReceiveResponseInstrumenter() {
        return sqlClientInstrumenter;
    }

    // From io.vertx.sqlclient.impl.tracing.QueryTracer
    static class QueryTrace {
        private final Map<String, String> attributes;

        QueryTrace(final Map<String, String> attributes) {
            this.attributes = attributes;
        }

        static QueryTrace queryTrace(final Map<String, String> attributes) {
            return new QueryTrace(attributes);
        }

        public String rawStatement() {
            return attributes.get("db.statement");
        }

        public String system() {
            return attributes.get("db.instance");
        }

        public String user() {
            return attributes.get("db.user");
        }

        public String connectionString() {
            return attributes.get("peer.address");
        }
    }

    static class SqlClientAttributesExtractor extends SqlAttributesExtractor<QueryTrace, QueryTrace> {
        @Override
        protected AttributeKey<String> dbTableAttribute() {
            return null;
        }

        @Override
        protected String rawStatement(final QueryTrace queryTrace) {
            return queryTrace.rawStatement();
        }

        @Override
        protected String system(final QueryTrace queryTrace) {
            return queryTrace.system();
        }

        @Override
        protected String user(final QueryTrace queryTrace) {
            return queryTrace.user();
        }

        @Override
        protected String name(final QueryTrace queryTrace) {
            return null;
        }

        @Override
        protected String connectionString(final QueryTrace queryTrace) {
            return queryTrace.connectionString();
        }
    }
}
