package io.quarkus.funqy.runtime.bindings.knative.events;

import static io.quarkus.funqy.runtime.bindings.knative.events.KnativeEventsBindingRecorder.RESPONSE_SOURCE;
import static io.quarkus.funqy.runtime.bindings.knative.events.KnativeEventsBindingRecorder.RESPONSE_TYPE;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

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
import io.quarkus.funqy.runtime.FunctionInvoker;
import io.quarkus.funqy.runtime.FunctionRecorder;
import io.quarkus.funqy.runtime.FunqyServerResponse;
import io.quarkus.funqy.runtime.RequestContextImpl;
import io.quarkus.funqy.runtime.query.QueryReader;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
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
    protected final Map<String, FunctionInvoker> typeTriggers;
    protected final String rootPath;

    public VertxRequestHandler(Vertx vertx,
            String rootPath,
            BeanContainer beanContainer,
            ObjectMapper mapper,
            FunqyKnativeEventsConfig config,
            FunctionInvoker defaultInvoker,
            Map<String, FunctionInvoker> typeTriggers,
            Executor executor) {
        this.rootPath = rootPath;
        this.defaultInvoker = defaultInvoker;
        this.vertx = vertx;
        this.beanContainer = beanContainer;
        this.executor = executor;
        this.mapper = mapper;
        this.typeTriggers = typeTriggers;
        Instance<CurrentIdentityAssociation> association = CDI.current().select(CurrentIdentityAssociation.class);
        this.association = association.isResolvable() ? association.get() : null;
        this.currentVertxRequest = CDI.current().select(CurrentVertxRequest.class).get();
    }

    @Override
    public void handle(RoutingContext routingContext) {
        String mediaType = routingContext.request().getHeader("Content-Type");
        if (mediaType == null || mediaType.startsWith("application/json") || mediaType.trim().equals("")) {
            if (routingContext.request().getHeader("ce-id") != null) {
                binaryContentMode(routingContext);
            } else {
                regularFunqyHttp(routingContext);
            }
        } else if (mediaType.startsWith("application/cloudevents+json")) {
            structuredMode(routingContext);
        } else if (mediaType.startsWith("application/cloudevents-batch+json")) {
            routingContext.fail(406);
            log.error("Batch mode not supported yet");
            return;

        } else {
            routingContext.fail(406);
            log.error("Illegal media type:" + mediaType);
            return;
        }
    }

    private static final ResponseProcessing NOOP = () -> {
    };

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
        processHttpRequest(null, routingContext, NOOP, invoker);
    }

    private void binaryContentMode(RoutingContext routingContext) {
        String ceType = routingContext.request().getHeader("ce-type");
        FunctionInvoker invoker = defaultInvoker;
        if (invoker == null) {
            // map by type trigger
            invoker = typeTriggers.get(ceType);
            if (invoker == null) {
                invoker = typeTriggers.get("*"); // Catch-all type
            }
            if (invoker == null) {
                routingContext.fail(404);
                log.error("Could not map ce-type header: " + ceType + " to a function");
                return;
            }

        }
        final FunctionInvoker targetInvoker = invoker;
        processHttpRequest(new HeaderCloudEventImpl(routingContext.request()), routingContext, () -> {
            routingContext.response().putHeader("ce-id", getResponseId());
            routingContext.response().putHeader("ce-specversion", "1.0");
            routingContext.response().putHeader("ce-source",
                    (String) targetInvoker.getBindingContext().get(RESPONSE_SOURCE));
            routingContext.response().putHeader("ce-type",
                    (String) targetInvoker.getBindingContext().get(RESPONSE_TYPE));
        }, invoker);
    }

    @FunctionalInterface
    interface ResponseProcessing {
        void handle();
    }

    private void processHttpRequest(CloudEvent event, RoutingContext routingContext, ResponseProcessing handler,
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
                execute(event, routingContext, handler, invoker, input);
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
                        ObjectReader reader = (ObjectReader) invoker.getBindingContext().get(ObjectReader.class.getName());
                        try {
                            input = reader.readValue((InputStream) in);
                        } catch (JsonProcessingException e) {
                            log.error("Failed to unmarshal input", e);
                            routingContext.fail(400);
                            return;
                        }
                    }
                    execute(event, routingContext, handler, invoker, input);
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

    private void execute(CloudEvent event, RoutingContext routingContext, ResponseProcessing handler, FunctionInvoker invoker,
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
                                    handler.handle();
                                    ObjectWriter writer = (ObjectWriter) invoker.getBindingContext()
                                            .get(ObjectWriter.class.getName());
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

    private void structuredMode(RoutingContext routingContext) {
        if (routingContext.request().method() != HttpMethod.POST) {
            routingContext.fail(405);
            log.error("Must be POST method");
            return;
        }
        routingContext.request().bodyHandler(buff -> {
            try {
                ByteBufInputStream in = new ByteBufInputStream(buff.getByteBuf());
                Object input = null;
                JsonNode event;
                try {
                    event = mapper.reader().readTree((InputStream) in);
                } catch (JsonProcessingException e) {
                    log.error("Failed to unmarshal input", e);
                    routingContext.fail(400);
                    return;
                }
                FunctionInvoker invoker = defaultInvoker;
                if (invoker == null) {
                    String eventType = event.get("type").asText();
                    invoker = typeTriggers.get(eventType);
                    if (invoker == null) {
                        routingContext.fail(404);
                        log.error("Could not map json cloud event to function: " + eventType);
                        return;
                    }

                }
                final FunctionInvoker targetInvoker = invoker;
                if (invoker.hasInput()) {

                    JsonNode dct = event.get("datacontenttype");
                    if (dct == null) {
                        routingContext.fail(400);
                        return;
                    }
                    String type = dct.asText();
                    if (type != null) {
                        if (!type.equals("application/json")) {
                            routingContext.fail(406);
                            log.error("Illegal datacontenttype");
                            return;
                        }
                        JsonNode data = event.get("data");
                        if (data != null) {
                            ObjectReader reader = (ObjectReader) invoker.getBindingContext().get(ObjectReader.class.getName());
                            try {
                                input = reader.readValue(data);
                            } catch (JsonProcessingException e) {
                                log.error("Failed to unmarshal input", e);
                                routingContext.fail(400);
                                return;
                            }
                        }
                    }
                }
                Object finalInput = input;

                executor.execute(() -> {
                    try {
                        final HttpServerResponse httpResponse = routingContext.response();
                        final FunqyServerResponse response = dispatch(new JsonCloudEventImpl(event), routingContext,
                                targetInvoker, finalInput);

                        response.getOutput().emitOn(executor).subscribe().with(
                                obj -> {
                                    if (targetInvoker.hasOutput()) {
                                        httpResponse.setStatusCode(200);
                                        final Map<String, Object> responseEvent = new HashMap<>();

                                        responseEvent.put("id", getResponseId());
                                        responseEvent.put("specversion", "1.0");
                                        responseEvent.put("source",
                                                targetInvoker.getBindingContext().get(RESPONSE_SOURCE));
                                        responseEvent.put("type",
                                                targetInvoker.getBindingContext().get(RESPONSE_TYPE));
                                        responseEvent.put("datacontenttype", "application/json");
                                        responseEvent.put("data", obj);
                                        try {
                                            httpResponse.end(mapper.writer().writeValueAsString(responseEvent));
                                        } catch (JsonProcessingException e) {
                                            log.error("Failed to marshal", e);
                                            routingContext.fail(400);
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
            ((Consumer<Uni<SecurityIdentity>>) association).accept(QuarkusHttpUser.getSecurityIdentity(routingContext, null));
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
