package io.quarkus.funqy.runtime.bindings.knative;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.netty.buffer.ByteBufInputStream;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.funqy.runtime.FunctionInvoker;
import io.quarkus.funqy.runtime.FunqyServerResponse;
import io.quarkus.funqy.runtime.RequestContextImpl;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

public class VertxRequestHandler implements Handler<RoutingContext> {
    private static final Logger log = Logger.getLogger("io.quarkus.funqy");

    protected final Vertx vertx;
    protected final ObjectMapper mapper;
    protected final FunctionInvoker invoker;
    protected final BeanContainer beanContainer;
    protected final CurrentIdentityAssociation association;
    protected final CurrentVertxRequest currentVertxRequest;
    protected final Executor executor;

    public VertxRequestHandler(Vertx vertx,
            BeanContainer beanContainer,
            FunctionInvoker invoker,
            ObjectMapper mapper,
            Executor executor) {
        this.vertx = vertx;
        this.beanContainer = beanContainer;
        this.invoker = invoker;
        this.executor = executor;
        this.mapper = mapper;
        Instance<CurrentIdentityAssociation> association = CDI.current().select(CurrentIdentityAssociation.class);
        this.association = association.isResolvable() ? association.get() : null;
        currentVertxRequest = CDI.current().select(CurrentVertxRequest.class).get();
    }

    @Override
    public void handle(RoutingContext routingContext) {
        if (routingContext.request().method() != HttpMethod.POST) {
            routingContext.fail(405);
            log.error("Must be POST method");
            return;
        }
        String mediaType = routingContext.request().getHeader("Content-Type");
        if (mediaType == null || mediaType.startsWith("application/json")) {
            if (routingContext.request().getHeader("ce-id") != null) {
                vanillaMode(routingContext, () -> {
                    routingContext.response().putHeader("ce-id", getResponseId());
                    routingContext.response().putHeader("ce-specversion", "1.0");
                    routingContext.response().putHeader("ce-source", getResponseSource());
                    routingContext.response().putHeader("ce-type", getResponseType());
                });

            } else {
                vanillaMode(routingContext, () -> {
                });
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

    @FunctionalInterface
    interface ResponseProcessing {
        void handle();
    }

    private void vanillaMode(RoutingContext routingContext, ResponseProcessing handler) {
        routingContext.request().bodyHandler(buff -> {
            Object input = null;
            if (buff.length() > 0) {
                ByteBufInputStream in = new ByteBufInputStream(buff.getByteBuf());
                ObjectReader reader = (ObjectReader) invoker.getBindingContext().get(ObjectReader.class.getName());
                try {
                    input = reader.readValue((InputStream) in);
                } catch (Exception e) {
                    log.error("Failed to unmarshal input", e);
                    routingContext.fail(400);
                    return;
                }
            }
            Object finalInput = input;
            executor.execute(() -> {
                try {
                    FunqyServerResponse response = dispatch(routingContext, invoker, finalInput);
                    if (invoker.hasOutput()) {
                        routingContext.response().setStatusCode(200);
                        handler.handle();
                        ObjectWriter writer = (ObjectWriter) invoker.getBindingContext().get(ObjectWriter.class.getName());
                        routingContext.response().putHeader("Content-Type", "application/json");
                        routingContext.response().end(writer.writeValueAsString(response.getOutput()));
                    } else {
                        routingContext.response().setStatusCode(204);
                        handler.handle();
                        routingContext.response().end();
                    }
                } catch (Exception e) {
                    routingContext.fail(e);
                }
            });
        });
    }

    private void structuredMode(RoutingContext routingContext) {
        routingContext.request().bodyHandler(buff -> {
            ByteBufInputStream in = new ByteBufInputStream(buff.getByteBuf());
            try {
                Object input = null;
                JsonNode event = mapper.reader().readTree((InputStream) in);
                String type = event.get("datacontenttype").asText();
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
                        } catch (Exception e) {
                            log.error("Failed to unmarshal input", e);
                            routingContext.fail(400);
                            return;
                        }
                    }

                }
                Object finalInput = input;
                executor.execute(() -> {
                    FunqyServerResponse response = dispatch(routingContext, invoker, finalInput);
                    Map<String, Object> responseEvent = new HashMap<>();
                    responseEvent.put("id", getResponseId());
                    responseEvent.put("specversion", "1.0");
                    responseEvent.put("source", getResponseSource());
                    responseEvent.put("type", getResponseType());
                    routingContext.response().setStatusCode(200);
                    if (invoker.hasOutput()) {
                        responseEvent.put("datacontenttype", "application/json");
                        responseEvent.put("data", response.getOutput());
                        ObjectWriter writer = (ObjectWriter) invoker.getBindingContext().get(ObjectWriter.class.getName());
                        try {
                            routingContext.response().end(mapper.writer().writeValueAsString(responseEvent));
                        } catch (Exception e) {
                            log.error("Failed to marshal", e);
                            routingContext.fail(e);
                        }
                    } else {
                        try {
                            routingContext.response().end(mapper.writer().writeValueAsString(responseEvent));
                        } catch (Exception e) {
                            routingContext.fail(e);
                        }
                    }

                });
            } catch (IOException e) {
                log.error("Failed to unmarshal input", e);
                routingContext.fail(400);
                return;
            }
        });
    }

    private String getResponseType() {
        return invoker.getName();
    }

    private String getResponseSource() {
        return "dev.knative." + invoker.getName();
    }

    private String getResponseId() {
        return UUID.randomUUID().toString();
    }

    private FunqyServerResponse dispatch(RoutingContext routingContext, FunctionInvoker invoker, Object input) {
        ManagedContext requestContext = beanContainer.requestContext();
        requestContext.activate();
        QuarkusHttpUser user = (QuarkusHttpUser) routingContext.user();
        if (user != null && association != null) {
            association.setIdentity(user.getSecurityIdentity());
        }
        currentVertxRequest.setCurrent(routingContext);
        try {
            FunqyRequestImpl funqyRequest = new FunqyRequestImpl(new RequestContextImpl(), input);
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
