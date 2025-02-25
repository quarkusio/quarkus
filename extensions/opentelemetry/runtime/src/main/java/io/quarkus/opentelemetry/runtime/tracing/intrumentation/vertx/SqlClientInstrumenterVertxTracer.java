package io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx;

import static io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig.INSTRUMENTATION_NAME;

import java.util.Map;
import java.util.function.BiConsumer;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.semconv.db.DbClientSpanNameExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.db.SqlClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.vertx.core.Context;
import io.vertx.core.spi.tracing.SpanKind;
import io.vertx.core.spi.tracing.TagExtractor;
import io.vertx.core.tracing.TracingPolicy;

public class SqlClientInstrumenterVertxTracer implements
        InstrumenterVertxTracer<SqlClientInstrumenterVertxTracer.QueryTrace, SqlClientInstrumenterVertxTracer.QueryTrace> {
    private final Instrumenter<QueryTrace, QueryTrace> sqlClientInstrumenter;

    public SqlClientInstrumenterVertxTracer(final OpenTelemetry openTelemetry, final OTelRuntimeConfig runtimeConfig) {
        SqlClientAttributesGetter sqlClientAttributesGetter = new SqlClientAttributesGetter();

        InstrumenterBuilder<QueryTrace, QueryTrace> serverBuilder = Instrumenter.builder(
                openTelemetry,
                INSTRUMENTATION_NAME, DbClientSpanNameExtractor.create(sqlClientAttributesGetter));

        serverBuilder.setEnabled(!runtimeConfig.sdkDisabled());

        this.sqlClientInstrumenter = serverBuilder
                .addAttributesExtractor(SqlClientAttributesExtractor.create(sqlClientAttributesGetter))
                .buildClientInstrumenter((queryTrace, key, value) -> {
                });
    }

    @Override
    public <R> boolean canHandle(final R request, final TagExtractor<R> tagExtractor) {
        if (request instanceof QueryTrace) {
            return true;
        }

        return "sql".equals(tagExtractor.extract(request).get("db.type"));
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

    // From io.vertx.sqlclient.impl.tracing.QueryReporter
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

    static class SqlClientAttributesGetter implements
            io.opentelemetry.instrumentation.api.incubator.semconv.db.SqlClientAttributesGetter<QueryTrace> {

        @Override
        public String getRawStatement(final QueryTrace queryTrace) {
            return queryTrace.rawStatement();
        }

        @Override
        public String getSystem(final QueryTrace queryTrace) {
            return queryTrace.system();
        }

        @Override
        public String getUser(final QueryTrace queryTrace) {
            return queryTrace.user();
        }

        @Override
        public String getName(final QueryTrace queryTrace) {
            return null;
        }

        @Override
        public String getConnectionString(final QueryTrace queryTrace) {
            return queryTrace.connectionString();
        }
    }
}
