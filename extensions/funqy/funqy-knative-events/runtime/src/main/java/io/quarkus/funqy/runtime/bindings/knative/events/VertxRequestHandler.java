package io.quarkus.funqy.runtime.bindings.knative.events;

import static io.quarkus.funqy.runtime.bindings.knative.events.KnativeEventsBindingRecorder.RESPONSE_SOURCE;
import static io.quarkus.funqy.runtime.bindings.knative.events.KnativeEventsBindingRecorder.RESPONSE_TYPE;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.cloudevents.CloudEvent;
import io.cloudevents.SpecVersion;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.impl.CloudEventUtils;
import io.cloudevents.core.message.MessageReader;
import io.cloudevents.core.message.MessageWriter;
import io.cloudevents.http.HttpMessageFactory;
import io.cloudevents.jackson.PojoCloudEventData;
import io.cloudevents.jackson.PojoCloudEventDataMapper;
import io.netty.buffer.ByteBufInputStream;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.runtime.BeanContainer;
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

    private static Uni<MessageReader> messageReader(RoutingContext routingContext) {
        return Uni.createFrom().emitter(uniEmitter -> {
            try {
                final HttpServerRequest request = routingContext.request();
                final Consumer<BiConsumer<String, String>> forEachHeader = processHeader -> {
                    request.headers().forEach(kvp -> processHeader.accept(kvp.getKey(), kvp.getValue()));
                };

                request.bodyHandler(body -> {
                    uniEmitter.complete(HttpMessageFactory.createReader(forEachHeader, body.getBytes()));
                });
            } catch (Throwable t) {
                uniEmitter.fail(t);
            }
        });
    }

    private static MessageWriter messageWriter(RoutingContext routingContext) {
        final HttpServerResponse response = routingContext.response();
        final BiConsumer<String, String> putHeader = (k, v) -> response.putHeader(k, v);

        return HttpMessageFactory.createWriter(putHeader, body -> {
            if (body != null && body.length > 0) {
                response.end(Buffer.buffer(body));
            } else {
                response.setStatusCode(204);
                response.end();
            }
        });
    }

    @Override
    public void handle(RoutingContext routingContext) {
        final HttpServerRequest request = routingContext.request();
        final String mediaType = request.getHeader("Content-Type");
        boolean binaryCE = request.headers().contains("Ce-Id");
        boolean structuredCE = false;
        if (mediaType != null) {
            structuredCE = mediaType.startsWith("application/cloudevents+");
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
        final HttpServerRequest request = routingContext.request();
        final String mediaType = request.getHeader("Content-Type");
        final boolean binaryCE = request.headers().contains("Ce-Id");
        final Consumer<Throwable> fail = t -> routingContext.fail(t);

        messageReader(routingContext)
                .emitOn(executor)
                .subscribe()
                .with(messageReader -> {
                    try {
                        CloudEvent inputCE = messageReader.toEvent();

                        final String ceType = inputCE.getType();
                        final FunctionInvoker invoker;
                        if (defaultInvoker != null) {
                            invoker = defaultInvoker;
                        } else {
                            invoker = typeTriggers.get(ceType);
                            if (invoker == null) {
                                routingContext.fail(404);
                                log.error("Could CloudEvent type: " + ceType + " to a function");
                                return;
                            }
                        }

                        Object inputData = null;
                        if (invoker.hasInput()) {
                            PojoCloudEventDataMapper<?> ceDataMapper = PojoCloudEventDataMapper.from(mapper,
                                    invoker.getInputType());
                            PojoCloudEventData<?> cloudEventData = CloudEventUtils.mapData(inputCE, ceDataMapper);
                            inputData = cloudEventData.getValue();
                        }

                        Consumer<Object> sendData = outputData -> {
                            try {

                                if (!invoker.hasOutput()) {
                                    routingContext.response().setStatusCode(204);
                                    routingContext.response().end();
                                    return;
                                }

                                String type = (String) invoker.getBindingContext().get(RESPONSE_TYPE);
                                String source = (String) invoker.getBindingContext().get(RESPONSE_SOURCE);
                                CloudEventBuilder builder = inputCE.getSpecVersion() == SpecVersion.V03
                                        ? CloudEventBuilder.v03()
                                        : CloudEventBuilder.v1();
                                final CloudEvent outputCE = builder
                                        .withId(getResponseId())
                                        .withSource(URI.create(source))
                                        .withType(type)
                                        .withData("application/json", mapper.writeValueAsBytes(outputData))
                                        .build();
                                MessageWriter mw = messageWriter(routingContext);
                                if (binaryCE) {
                                    mw.writeBinary(outputCE);
                                } else {
                                    mw.writeStructured(outputCE, mediaType);
                                }
                            } catch (Throwable t) {
                                routingContext.fail(t);
                            }
                        };

                        dispatch(inputCE, routingContext, invoker, inputData)
                                .getOutput()
                                .subscribe()
                                .with(sendData, fail);
                    } catch (Throwable t) {
                        routingContext.fail(t);
                    }
                }, fail);
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
        processHttpRequest(null, routingContext, invoker);
    }

    @FunctionalInterface
    interface ResponseProcessing {
        void handle();
    }

    private void processHttpRequest(CloudEvent event, RoutingContext routingContext, FunctionInvoker invoker) {
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
                        ObjectReader reader = (ObjectReader) invoker.getBindingContext().get(ObjectReader.class.getName());
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

    private void execute(CloudEvent event, RoutingContext routingContext,
            FunctionInvoker invoker,
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

    private String getResponseId() {
        return UUID.randomUUID().toString();
    }

    private FunqyServerResponse dispatch(CloudEvent event, RoutingContext routingContext,
            FunctionInvoker invoker,
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
