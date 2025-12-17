package io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx;

import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_NAMESPACE;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DbSystemNameIncubatingValues.REDIS;
import static io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig.INSTRUMENTATION_NAME;

import java.util.Map;
import java.util.function.BiConsumer;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.instrumentation.api.incubator.semconv.db.DbClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.db.DbClientAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.db.DbClientSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.internal.AttributesExtractorUtil;
import io.opentelemetry.instrumentation.api.semconv.network.NetworkAttributesExtractor;
import io.opentelemetry.instrumentation.api.semconv.network.NetworkAttributesGetter;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.vertx.core.Context;
import io.vertx.core.spi.tracing.SpanKind;
import io.vertx.core.spi.tracing.TagExtractor;
import io.vertx.core.tracing.TracingPolicy;

public class RedisClientInstrumenterVertxTracer implements
        InstrumenterVertxTracer<RedisClientInstrumenterVertxTracer.CommandTrace, Object> {
    private final Instrumenter<CommandTrace, Object> redisClientInstrumenter;

    public RedisClientInstrumenterVertxTracer(final OpenTelemetry openTelemetry, final OTelRuntimeConfig runtimeConfig) {
        InstrumenterBuilder<CommandTrace, Object> clientInstrumenterBuilder = Instrumenter.builder(
                openTelemetry,
                INSTRUMENTATION_NAME,
                DbClientSpanNameExtractor.create(RedisClientAttributesGetter.INSTANCE));

        clientInstrumenterBuilder.setEnabled(!runtimeConfig.sdkDisabled());

        this.redisClientInstrumenter = clientInstrumenterBuilder
                .addAttributesExtractor(DbClientAttributesExtractor.create(RedisClientAttributesGetter.INSTANCE))
                .addAttributesExtractor(NetworkAttributesExtractor.create(RedisClientAttributesGetter.INSTANCE))
                .addAttributesExtractor(RedisClientAttributesExtractor.INSTANCE)
                .buildInstrumenter(SpanKindExtractor.alwaysClient());
    }

    @Override
    public <R> boolean canHandle(R request, TagExtractor<R> tagExtractor) {
        if (request instanceof CommandTrace) {
            return true;
        }

        return "redis".equals(tagExtractor.extract(request).get("db.type"));
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
        R commandTrace = (R) CommandTrace.commandTrace(tagExtractor.extract(request));
        return InstrumenterVertxTracer.super.sendRequest(context, kind, policy, commandTrace, operation, headers, tagExtractor);
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
    public Instrumenter<CommandTrace, Object> getReceiveRequestInstrumenter() {
        return null;
    }

    @Override
    public Instrumenter<CommandTrace, Object> getSendResponseInstrumenter() {
        return null;
    }

    @Override
    public Instrumenter<CommandTrace, Object> getSendRequestInstrumenter() {
        return redisClientInstrumenter;
    }

    @Override
    public Instrumenter<CommandTrace, Object> getReceiveResponseInstrumenter() {
        return redisClientInstrumenter;
    }

    static class CommandTrace {

        // From io.vertx.redis.client.impl.CommandReporter.Tags
        static final String DB_STATEMENT = "db.statement";
        static final String DB_USER = "db.user";
        static final String NETWORK_PEER_ADDRESS = "network.peer.address";
        static final String DB_INSTANCE = "db.instance";
        static final String NETWORK_PEER_PORT = "network.peer.port";
        static final String SERVER_ADDRESS = "server.address";
        static final String SERVER_PORT = "server.port";
        static final String ADDRESS_PORT = "peer.address";

        private final Map<String, String> attributes;

        CommandTrace(final Map<String, String> attributes) {
            this.attributes = attributes;
        }

        static CommandTrace commandTrace(final Map<String, String> attributes) {
            return new CommandTrace(attributes);
        }

        public String operation() {
            return attributes.get(DB_STATEMENT);
        }

        public String user() {
            return attributes.get(DB_USER);
        }

        @Deprecated
        public String serverAddressPort() {
            return attributes.get(ADDRESS_PORT);
        }

        public String peerAddress() {
            return attributes.get(NETWORK_PEER_ADDRESS);
        }

        public Integer peerPort() {
            return parsePort(attributes.get(NETWORK_PEER_PORT));
        }

        public String serverAddress() {
            return attributes.get(SERVER_ADDRESS);
        }

        public Integer serverPort() {
            return parsePort(attributes.get(SERVER_PORT));
        }

        public String dbIndex() {
            return attributes.get(DB_INSTANCE);
        }

        static int parsePort(String port) {
            if (port == null) {
                return -1;
            } else {
                try {
                    return Integer.parseInt(port);
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
    }

    enum RedisClientAttributesGetter implements DbClientAttributesGetter<CommandTrace, Object>,
            NetworkAttributesGetter<CommandTrace, Object> {
        INSTANCE;

        @Override
        public String getDbQueryText(final CommandTrace commandTrace) {
            return null;
        }

        @Override
        public String getDbOperationName(final CommandTrace commandTrace) {
            return commandTrace.operation();
        }

        @Override
        public String getDbSystem(final CommandTrace commandTrace) {
            return REDIS;
        }

        // kept for compatibility reasons
        @Deprecated
        @Override
        public String getUser(CommandTrace commandTrace) {
            return commandTrace.user();
        }

        @Override
        public String getDbNamespace(CommandTrace commandTrace) {
            return null;
        }

        // kept for compatibility reasons
        @Deprecated
        @Override
        public String getConnectionString(final CommandTrace commandTrace) {
            return commandTrace.serverAddressPort();
        }

        @Override
        public String getNetworkLocalAddress(CommandTrace commandTrace, Object object) {
            return commandTrace.serverAddress();
        }

        @Override
        public Integer getNetworkLocalPort(CommandTrace commandTrace, Object object) {
            return commandTrace.serverPort();
        }

        @Override
        public String getNetworkPeerAddress(CommandTrace commandTrace, Object object) {
            return commandTrace.peerAddress();
        }

        @Override
        public Integer getNetworkPeerPort(CommandTrace commandTrace, Object object) {
            return commandTrace.peerPort();
        }
    }

    enum RedisClientAttributesExtractor implements AttributesExtractor<CommandTrace, Object> {
        INSTANCE;

        @Override
        public void onStart(AttributesBuilder attributes, io.opentelemetry.context.Context parentContext,
                CommandTrace request) {
            AttributesExtractorUtil.internalSet(attributes, DB_NAMESPACE, request.dbIndex());
        }

        @Override
        public void onEnd(AttributesBuilder attributes,
                io.opentelemetry.context.Context context,
                CommandTrace request,
                Object response,
                Throwable error) {
        }
    }
}
