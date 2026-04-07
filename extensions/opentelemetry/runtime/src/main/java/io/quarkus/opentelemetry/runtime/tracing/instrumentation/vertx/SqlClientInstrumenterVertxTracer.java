package io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx;

import static io.opentelemetry.instrumentation.api.incubator.semconv.db.SqlDialect.DOUBLE_QUOTES_ARE_IDENTIFIERS;
import static io.opentelemetry.instrumentation.api.incubator.semconv.db.SqlDialect.DOUBLE_QUOTES_ARE_STRING_LITERALS;
import static io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig.INSTRUMENTATION_NAME;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.semconv.db.DbClientSpanNameExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.db.SqlClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.db.SqlDialect;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.semconv.network.NetworkAttributesExtractor;
import io.opentelemetry.instrumentation.api.semconv.network.NetworkAttributesGetter;
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
                .addAttributesExtractor(NetworkAttributesExtractor.create(sqlClientAttributesGetter))
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

    static class QueryTrace {

        // From io.vertx.sqlclient.impl.tracing.QueryReporter.TAGS
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

        @Deprecated
        public String user() {
            return attributes.get("db.user");
        }

        public String instance() {
            return attributes.get("db.instance");
        }

        @Deprecated
        public String connectionString() {
            return attributes.get("peer.address");
        }

        public String peerAddress() {
            return VertxUtil.extractHostname(attributes.get("peer.address"));
        }

        public Integer peerPort() {
            return VertxUtil.extractPort(attributes.get("peer.address"));
        }
    }

    static class SqlClientAttributesGetter implements
            io.opentelemetry.instrumentation.api.incubator.semconv.db.SqlClientAttributesGetter<QueryTrace, Object>,
            NetworkAttributesGetter<QueryTrace, Object> {

        // Databases where double quotes are exclusively identifiers and cannot be string literals.
        private static final Set<String> DOUBLE_QUOTES_FOR_IDENTIFIERS_SYSTEMS = Set.of(
                // "A string constant in SQL is an arbitrary sequence of characters
                // bounded by single quotes (')"
                // https://www.postgresql.org/docs/current/sql-syntax-lexical.html#SQL-SYNTAX-STRINGS
                "postgresql",
                // "Text, character, and string literals are always surrounded
                // by single quotation marks."
                // https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Literals.html
                "oracle",
                // "A sequence of characters that starts and ends with a string delimiter,
                // which is an apostrophe (')"
                // https://www.ibm.com/docs/en/db2/12.1?topic=elements-constants
                "db2",
                // "Single quotation marks delimit character strings."
                // "Double quotation marks delimit special identifiers"
                // https://db.apache.org/derby/docs/10.17/ref/rrefsqlj28468.html
                "derby",
                // "names of objects are enclosed in double-quotes"
                // (double quotes are exclusively for identifiers; follows SQL standard strictly)
                // https://hsqldb.org/doc/2.0/guide/sqlgeneral-chapt.html
                "hsqldb",
                // <string_literal> ::= <single_quote>[<any_character>...]<single_quote>
                // <special_identifier> ::= <double_quotes><any_character>...<double_quotes>
                // https://help.sap.com/docs/hana-cloud-database/sap-hana-cloud-sap-hana-database-sql-reference-guide/sql-notation-conventions
                "hanadb",
                // "String literals must be enclosed in single quotes.
                // Double quotes are not supported."
                // https://clickhouse.com/docs/en/sql-reference/syntax#string
                "clickhouse",
                // PostgreSQL-compatible fork, inherits PG string literal rules
                "polardb");

        /**
         * Same as in
         * io.opentelemetry.instrumentation.jdbc.internal.JdbcAttributesGetter#getSqlDialect(io.opentelemetry.instrumentation.jdbc.internal.DbRequest)
         */
        @Override
        public SqlDialect getSqlDialect(QueryTrace queryTrace) {
            String system = queryTrace.system();
            if (system != null && DOUBLE_QUOTES_FOR_IDENTIFIERS_SYSTEMS.contains(system)) {
                return DOUBLE_QUOTES_ARE_IDENTIFIERS;
            }
            // default to treating double-quoted tokens as string literals for safety, ensuring that
            // potentially sensitive values are not captured
            return DOUBLE_QUOTES_ARE_STRING_LITERALS;
        }

        @Override
        public Collection<String> getRawQueryTexts(final QueryTrace queryTrace) {
            return queryTrace.rawStatement() != null && !queryTrace.rawStatement().isBlank()
                    ? List.of(queryTrace.rawStatement())
                    : Collections.emptyList();
        }

        @Override
        public String getDbSystemName(final QueryTrace queryTrace) {
            return queryTrace.system();
        }

        // kept for compatibility reasons
        @Deprecated
        @Override
        public String getUser(final QueryTrace queryTrace) {
            return queryTrace.user();
        }

        @Override
        public String getDbNamespace(final QueryTrace queryTrace) {
            return null;
        }

        // kept for compatibility reasons
        @Deprecated
        @Override
        public String getConnectionString(final QueryTrace queryTrace) {
            return queryTrace.connectionString();
        }

        @Override
        public String getNetworkPeerAddress(QueryTrace queryTrace, Object object) {
            return queryTrace.peerAddress();
        }

        @Override
        public Integer getNetworkPeerPort(QueryTrace queryTrace, Object object) {
            return queryTrace.peerPort();
        }
    }
}
