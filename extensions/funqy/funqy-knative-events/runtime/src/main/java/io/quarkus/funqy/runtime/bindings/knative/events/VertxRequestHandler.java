package io.quarkus.funqy.runtime.bindings.knative.events;

import static io.quarkus.funqy.knative.events.AbstractCloudEvent.isKnownSpecVersion;
import static io.quarkus.funqy.knative.events.AbstractCloudEvent.parseMajorSpecVersion;
import static io.quarkus.funqy.runtime.bindings.knative.events.KnativeEventsBindingRecorder.DATA_OBJECT_READER;
import static io.quarkus.funqy.runtime.bindings.knative.events.KnativeEventsBindingRecorder.DATA_OBJECT_WRITER;
import static io.quarkus.funqy.runtime.bindings.knative.events.KnativeEventsBindingRecorder.INPUT_CE_DATA_TYPE;
import static io.quarkus.funqy.runtime.bindings.knative.events.KnativeEventsBindingRecorder.OUTPUT_CE_DATA_TYPE;
import static io.quarkus.funqy.runtime.bindings.knative.events.KnativeEventsBindingRecorder.RESPONSE_SOURCE;
import static io.quarkus.funqy.runtime.bindings.knative.events.KnativeEventsBindingRecorder.RESPONSE_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.netty.buffer.ByteBufInputStream;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventBuilder;
import io.quarkus.funqy.runtime.FunctionInvoker;
import io.quarkus.funqy.runtime.FunctionRecorder;
import io.quarkus.funqy.runtime.FunqyServerResponse;
import io.quarkus.funqy.runtime.RequestContextImpl;
import io.quarkus.funqy.runtime.query.QueryReader;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class VertxRequestHandler implements Handler<RoutingContext> {
    private static final Logger log = Logger.getLogger("io.quarkus.funqy");

    protected final Vertx vertx;
    protected final ObjectMapper mapper;
    protected final BeanContainer beanContainer;
    protected final CurrentIdentityAssociation association;
    protected final CurrentVertxRequest currentVertxRequest;
    protected final Executor executor;
    protected final FunctionInvoker defaultInvoker;
    protected final Map<String, Collection<FunctionInvoker>> typeTriggers;
    protected final Map<String, List<Predicate<CloudEvent>>> invokersFilters;
    protected final String rootPath;

    public VertxRequestHandler(Vertx vertx,
            String rootPath,
            BeanContainer beanContainer,
            ObjectMapper mapper,
            FunqyKnativeEventsConfig config,
            FunctionInvoker defaultInvoker,
            Map<String, Collection<FunctionInvoker>> typeTriggers,
            Map<String, List<Predicate<CloudEvent>>> invokersFilters,
            Executor executor) {
        this.rootPath = rootPath;
        this.defaultInvoker = defaultInvoker;
        this.vertx = vertx;
        this.beanContainer = beanContainer;
        this.executor = executor;
        this.mapper = mapper;
        this.typeTriggers = typeTriggers;
        this.invokersFilters = invokersFilters;
        Instance<CurrentIdentityAssociation> association = CDI.current().select(CurrentIdentityAssociation.class);
        this.association = association.isResolvable() ? association.get() : null;
        this.currentVertxRequest = CDI.current().select(CurrentVertxRequest.class).get();
    }

    @Override
    public void handle(RoutingContext routingContext) {
        final HttpServerRequest request = routingContext.request();
        final String mediaType = request.getHeader("Content-Type");
        boolean binaryCE = request.headers().contains("Ce-Id");
        boolean structuredCE = false;
        if (mediaType != null) {
            structuredCE = mediaType.startsWith("application/cloudevents+json");
        }

        if (structuredCE || binaryCE) {
            try {
                processCloudEvent(routingContext);
            } catch (Throwable t) {
                routingContext.fail(t);
            }
        } else if ((mediaType != null && mediaType.startsWith("application/json") && request.method() == HttpMethod.POST) ||
                request.method() == HttpMethod.GET) {
            regularFunqyHttp(routingContext);
        } else if (mediaType != null && mediaType.startsWith("application/cloudevents-batch+json")) {
            routingContext.fail(406);
            log.error("Batch mode not supported yet");
            return;

        } else {
            routingContext.fail(406);
            log.error("Illegal media type:" + mediaType);
            return;
        }
    }

    private void processCloudEvent(RoutingContext routingContext) {
        final HttpServerRequest httpRequest = routingContext.request();
        final HttpServerResponse httpResponse = routingContext.response();
        final boolean binaryCE = httpRequest.headers().contains("ce-id");

        httpRequest.bodyHandler(bodyBuff -> executor.execute(() -> {
            try {
                final String ceType;
                final String ceSpecVersion;
                final JsonNode structuredPayload;

                if (binaryCE) {
                    ceType = httpRequest.headers().get("ce-type");
                    ceSpecVersion = httpRequest.headers().get("ce-specversion");
                    structuredPayload = null;
                } else {
                    try {
                        structuredPayload = mapper.readTree(bodyBuff.getBytes());
                        ceType = structuredPayload.get("type").asText();
                        ceSpecVersion = structuredPayload.get("specversion").asText();
                    } catch (IOException e) {
                        routingContext.fail(e);
                        return;
                    }
                }

                if (!isKnownSpecVersion(ceSpecVersion)) {
                    log.warnf("Unexpected CloudEvent spec-version '%s'.", ceSpecVersion);
                }

                Collection<FunctionInvoker> candidates = new ArrayList<>();
                if (defaultInvoker != null) {
                    candidates.add(defaultInvoker);
                } else {
                    candidates = typeTriggers.get(ceType);

                    if (candidates == null || candidates.isEmpty()) {
                        candidates = typeTriggers.get("*"); // Catch-all
                    }

                    if (candidates == null || candidates.isEmpty()) {
                        routingContext.fail(404);
                        log.error("Couldn't map CloudEvent type: '" + ceType + "' to a function.");
                        return;
                    }
                }

                final CloudEvent<?> evt;
                if (binaryCE) {
                    evt = new HeaderCloudEventImpl<>(
                            httpRequest.headers(),
                            null,
                            null,
                            null,
                            null);
                } else {
                    evt = new JsonCloudEventImpl<>(
                            structuredPayload,
                            null,
                            null,
                            null);
                }
                List<FunctionInvoker> matchingCandidates = candidates
                        .stream()
                        .filter(fi -> match(fi, evt))
                        .collect(Collectors.toList());

                if (matchingCandidates.size() <= 0) {
                    routingContext.fail(404);
                    log.error("Couldn't map CloudEvent type: '" + ceType + "' to any function.");
                    return;
                }
                if (matchingCandidates.size() > 1) {
                    routingContext.fail(409);
                    log.error("CloudEvent type: '" + ceType + "' matches multiple function.");
                    return;
                }

                final FunctionInvoker invoker = matchingCandidates.get(0);

                final Type inputCeDataType = (Type) invoker.getBindingContext().get(INPUT_CE_DATA_TYPE);
                final Type outputCeDataType = (Type) invoker.getBindingContext().get(OUTPUT_CE_DATA_TYPE);
                final Type innerInputType = inputCeDataType != null ? inputCeDataType : invoker.getInputType();
                final Type innerOutputType = outputCeDataType != null ? outputCeDataType : invoker.getOutputType();
                final ObjectReader reader = (ObjectReader) invoker.getBindingContext().get(DATA_OBJECT_READER);
                final ObjectWriter writer = (ObjectWriter) invoker.getBindingContext().get(DATA_OBJECT_WRITER);

                final CloudEvent<?> inputCloudEvent;
                final Object input;
                if (invoker.hasInput()) {
                    if (binaryCE) {
                        inputCloudEvent = new HeaderCloudEventImpl<>(
                                httpRequest.headers(),
                                bodyBuff,
                                inputCeDataType != null ? inputCeDataType : innerInputType,
                                mapper,
                                reader);
                    } else {
                        inputCloudEvent = new JsonCloudEventImpl<>(
                                structuredPayload,
                                inputCeDataType != null ? inputCeDataType : innerInputType,
                                mapper,
                                reader);
                    }
                    if (inputCeDataType == null) {
                        // we need to unwrap user data from CloudEvent
                        input = inputCloudEvent.data();
                    } else {
                        // user is explicitly handling CloudEvent
                        input = inputCloudEvent;
                    }
                } else {
                    input = null;
                    inputCloudEvent = null;
                }

                final Consumer<Object> sendOutput = output -> {
                    try {
                        if (!invoker.hasOutput()) {
                            routingContext.response().setStatusCode(204);
                            routingContext.response().end();
                            return;
                        }

                        final CloudEvent<?> outputCloudEvent;
                        if (outputCeDataType == null) {
                            // we need to wrap user data into CloudEvent
                            CloudEventBuilder builder = CloudEventBuilder.create();
                            if (byte[].class.equals(innerOutputType)) {
                                outputCloudEvent = builder.build((byte[]) output, "application/octet-stream");
                            } else {
                                outputCloudEvent = builder.build(output);
                            }
                        } else {
                            // user is explicitly returning CloudEvent
                            outputCloudEvent = (CloudEvent<?>) output;
                        }

                        String id = outputCloudEvent.id();
                        if (id == null) {
                            id = getResponseId();
                        }
                        String specVersion;
                        if (outputCloudEvent.specVersion() != null) {
                            specVersion = outputCloudEvent.specVersion();
                        } else if (inputCloudEvent != null && inputCloudEvent.specVersion() != null) {
                            specVersion = inputCloudEvent.specVersion();
                        } else {
                            specVersion = "1.0";
                        }
                        String source = outputCloudEvent.source();
                        if (source == null) {
                            source = (String) invoker.getBindingContext().get(RESPONSE_SOURCE);
                        }
                        String type = outputCloudEvent.type();
                        if (type == null) {
                            type = (String) invoker.getBindingContext().get(RESPONSE_TYPE);
                        }

                        boolean ceHasData = !Void.class.equals(innerInputType);

                        int majorSpecVer = parseMajorSpecVersion(specVersion);

                        if (binaryCE) {
                            httpResponse.putHeader("ce-id", id);
                            httpResponse.putHeader("ce-specversion", specVersion);
                            httpResponse.putHeader("ce-source", source);
                            httpResponse.putHeader("ce-type", type);

                            if (outputCloudEvent.time() != null) {
                                httpResponse.putHeader("ce-time", outputCloudEvent.time().toString());
                            }

                            if (outputCloudEvent.subject() != null) {
                                httpResponse.putHeader("ce-subject", outputCloudEvent.subject());
                            }

                            if (outputCloudEvent.dataSchema() != null) {
                                String dsName = majorSpecVer == 0 ? "ce-schemaurl" : "ce-dataschema";
                                httpResponse.putHeader(dsName, outputCloudEvent.dataSchema());
                            }

                            outputCloudEvent.extensions()
                                    .entrySet()
                                    .forEach(e -> httpResponse.putHeader("ce-" + e.getKey(), e.getValue()));

                            String dataContentType = outputCloudEvent.dataContentType();
                            if (dataContentType != null) {
                                httpResponse.putHeader("Content-Type", dataContentType);
                            }

                            if (ceHasData) {
                                if (dataContentType != null && dataContentType.startsWith("application/json")) {
                                    httpResponse.end(Buffer.buffer(writer.writeValueAsBytes(outputCloudEvent.data())));
                                } else if (byte[].class.equals(innerOutputType)) {
                                    httpResponse.end(Buffer.buffer((byte[]) outputCloudEvent.data()));
                                } else {
                                    log.errorf("Don't know how to write ce to output (dataContentType: %s, javaType: %s).",
                                            dataContentType, innerOutputType);
                                    routingContext.fail(500);
                                    return;
                                }
                            } else {
                                routingContext.response().setStatusCode(204);
                                routingContext.response().end();
                            }
                            return;
                        } else {
                            final Map<String, Object> responseEvent = new HashMap<>();
                            responseEvent.put("id", id);
                            responseEvent.put("specversion", specVersion);
                            responseEvent.put("source", source);
                            responseEvent.put("type", type);

                            if (outputCloudEvent.time() != null) {
                                responseEvent.put("time", outputCloudEvent.time());
                            }

                            if (outputCloudEvent.subject() != null) {
                                responseEvent.put("subject", outputCloudEvent.subject());
                            }

                            if (outputCloudEvent.dataSchema() != null) {
                                String dsName = majorSpecVer == 0 ? "schemaurl" : "dataschema";
                                responseEvent.put(dsName, outputCloudEvent.dataSchema());
                            }

                            outputCloudEvent.extensions()
                                    .entrySet()
                                    .forEach(e -> responseEvent.put(e.getKey(), e.getValue()));

                            String dataContentType = outputCloudEvent.dataContentType();
                            if (dataContentType != null) {
                                responseEvent.put("datacontenttype", dataContentType);
                            }

                            if (ceHasData) {
                                if (majorSpecVer == 0) {
                                    if (dataContentType != null && dataContentType.startsWith("application/json")) {
                                        responseEvent.put("data", outputCloudEvent.data());
                                    } else if (byte[].class.equals(innerOutputType)) {
                                        responseEvent.put("datacontentencoding", "base64");
                                        responseEvent.put("data", (byte[]) outputCloudEvent.data());
                                    } else {
                                        log.errorf(
                                                "Don't know how to write ce to output (dataContentType: %s, javaType: %s).",
                                                dataContentType, innerOutputType);
                                        routingContext.fail(500);
                                        return;
                                    }
                                } else {
                                    if (dataContentType != null && dataContentType.startsWith("application/json")) {
                                        responseEvent.put("data", outputCloudEvent.data());
                                    } else if (byte[].class.equals(innerOutputType)) {
                                        responseEvent.put("data_base64", (byte[]) outputCloudEvent.data());
                                    } else {
                                        log.errorf(
                                                "Don't know how to write ce to output (dataContentType: %s, javaType: %s).",
                                                dataContentType, innerOutputType);
                                        routingContext.fail(500);
                                        return;
                                    }

                                }
                            }

                            routingContext.response().putHeader("Content-Type", "application/cloudevents+json");
                            httpResponse.end(Buffer.buffer(mapper.writer().writeValueAsBytes(responseEvent)));
                            return;
                        }
                    } catch (Throwable t) {
                        routingContext.fail(t);
                    }
                };

                dispatch(inputCloudEvent, routingContext, invoker, input)
                        .getOutput()
                        .subscribe()
                        .with(sendOutput, t -> routingContext.fail(t));

            } catch (Throwable t) {
                routingContext.fail(t);
            }
        }));

    }

    private boolean match(FunctionInvoker invoker, CloudEvent<?> inputCloudEvent) {

        if (invokersFilters.get(invoker.getName()) == null || invokersFilters.get(invoker.getName()).isEmpty()) {
            return true;
        }

        return invokersFilters.get(invoker.getName()).stream().allMatch(p -> p.test(inputCloudEvent));
    }

    private void regularFunqyHttp(RoutingContext routingContext) {
        String path = routingContext.request().path();
        if (path == null) {
            routingContext.fail(404);
            return;
        }
        // expects rootPath to end with '/'
        if (!path.startsWith(rootPath)) {
            routingContext.fail(404);
            return;
        }

        path = path.substring(rootPath.length());

        final FunctionInvoker invoker;
        if (!path.isEmpty()) {
            invoker = FunctionRecorder.registry.matchInvoker(path);
        } else {
            invoker = defaultInvoker;
        }

        if (invoker == null) {
            routingContext.fail(404);
            log.error("There is no function matching the path.");
            return;
        }

        if (invoker.getBindingContext().get(INPUT_CE_DATA_TYPE) != null ||
                invoker.getBindingContext().get(OUTPUT_CE_DATA_TYPE) != null) {
            routingContext.fail(400);
            log.errorf("Bad request: the '%s' function expects CloudEvent, but plain HTTP was received.",
                    invoker.getName());
            return;
        }

        processHttpRequest(null, routingContext, invoker);
    }

    private void processHttpRequest(CloudEvent event, RoutingContext routingContext,
            FunctionInvoker invoker) {
        if (routingContext.request().method() == HttpMethod.GET) {
            Object input = null;
            if (invoker.hasInput()) {
                QueryReader reader = (QueryReader) invoker.getBindingContext().get(QueryReader.class.getName());
                try {
                    input = reader.readValue(routingContext.request().params().iterator());
                } catch (Exception e) {
                    log.error("Failed to unmarshal input", e);
                    routingContext.fail(400);
                    return;
                }
            }
            try {
                execute(event, routingContext, invoker, input);
            } catch (Throwable t) {
                log.error(t);
                routingContext.fail(500, t);
            }
        } else if (routingContext.request().method() == HttpMethod.POST) {
            routingContext.request().bodyHandler(buff -> {
                try {
                    Object input = null;
                    if (buff.length() > 0) {
                        ByteBufInputStream in = new ByteBufInputStream(buff.getByteBuf());
                        ObjectReader reader = (ObjectReader) invoker.getBindingContext().get(DATA_OBJECT_READER);
                        try {
                            input = reader.readValue((InputStream) in);
                        } catch (JsonProcessingException e) {
                            log.error("Failed to unmarshal input", e);
                            routingContext.fail(400);
                            return;
                        }
                    }
                    execute(event, routingContext, invoker, input);
                } catch (Throwable t) {
                    log.error(t);
                    routingContext.fail(500, t);
                }
            });
        } else {
            routingContext.fail(405);
            log.error("Must be POST or GET for: " + invoker.getName());
        }

    }

    private void execute(CloudEvent event, RoutingContext routingContext, FunctionInvoker invoker,
            Object finalInput) {
        executor.execute(() -> {
            try {
                final HttpServerResponse httpResponse = routingContext.response();
                FunqyServerResponse response = dispatch(event, routingContext, invoker, finalInput);

                response.getOutput().emitOn(executor).subscribe().with(
                        obj -> {
                            if (invoker.hasOutput()) {
                                try {
                                    httpResponse.setStatusCode(200);
                                    ObjectWriter writer = (ObjectWriter) invoker.getBindingContext()
                                            .get(DATA_OBJECT_WRITER);
                                    httpResponse.putHeader("Content-Type", "application/json");
                                    httpResponse.end(writer.writeValueAsString(obj));
                                } catch (JsonProcessingException jpe) {
                                    log.error("Failed to unmarshal input", jpe);
                                    routingContext.fail(400);
                                } catch (Throwable e) {
                                    routingContext.fail(e);
                                }
                            } else {
                                httpResponse.setStatusCode(204);
                                httpResponse.end();
                            }
                        },
                        t -> routingContext.fail(t));

            } catch (Throwable t) {
                log.error(t);
                routingContext.fail(500, t);
            }
        });
    }

    private String getResponseId() {
        return UUID.randomUUID().toString();
    }

    private FunqyServerResponse dispatch(CloudEvent event, RoutingContext routingContext, FunctionInvoker invoker,
            Object input) {
        ManagedContext requestContext = beanContainer.requestContext();
        requestContext.activate();
        if (association != null) {
            QuarkusHttpUser existing = (QuarkusHttpUser) routingContext.user();
            if (existing != null) {
                SecurityIdentity identity = existing.getSecurityIdentity();
                association.setIdentity(identity);
            } else {
                association.setIdentity(QuarkusHttpUser.getSecurityIdentity(routingContext, null));
            }
        }
        currentVertxRequest.setCurrent(routingContext);
        try {
            RequestContextImpl funqContext = new RequestContextImpl();
            if (event != null) {
                funqContext.setContextData(CloudEvent.class, event);
            }
            FunqyRequestImpl funqyRequest = new FunqyRequestImpl(funqContext, input);
            FunqyResponseImpl funqyResponse = new FunqyResponseImpl();
            invoker.invoke(funqyRequest, funqyResponse);
            return funqyResponse;
        } finally {
            if (requestContext.isActive()) {
                requestContext.terminate();
            }
        }
    }
}
