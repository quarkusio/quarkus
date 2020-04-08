package io.quarkus.funqy.runtime.bindings.http;

import java.io.InputStream;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.netty.buffer.ByteBufInputStream;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.funqy.runtime.FunctionInvoker;
import io.quarkus.funqy.runtime.FunctionRecorder;
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
    protected final String rootPath;
    protected final BeanContainer beanContainer;
    protected final CurrentIdentityAssociation association;
    protected final CurrentVertxRequest currentVertxRequest;
    protected final Executor executor;

    public VertxRequestHandler(Vertx vertx,
            BeanContainer beanContainer,
            String rootPath,
            Executor executor) {
        this.vertx = vertx;
        this.beanContainer = beanContainer;
        if (rootPath == null) {
            this.rootPath = "/";
        } else {
            if (rootPath.startsWith("/")) {
                this.rootPath = rootPath;
            } else {
                this.rootPath = "/" + rootPath;
            }
        }
        this.executor = executor;
        Instance<CurrentIdentityAssociation> association = CDI.current().select(CurrentIdentityAssociation.class);
        this.association = association.isResolvable() ? association.get() : null;
        currentVertxRequest = CDI.current().select(CurrentVertxRequest.class).get();
    }

    @Override
    public void handle(RoutingContext request) {
        String path = request.request().path();
        if (path == null) {
            request.fail(404);
            return;
        }
        if (!path.startsWith(rootPath)) {
            request.fail(404);
            return;
        }
        path = path.substring(rootPath.length());

        FunctionInvoker invoker = FunctionRecorder.registry.matchInvoker(path);

        if (invoker == null) {
            request.fail(404);
            return;
        }

        if (request.request().method() != HttpMethod.POST) {
            request.fail(405);
            return;
        }

        request.request().bodyHandler(buff -> {
            Object input = null;
            if (buff.length() > 0) {
                ByteBufInputStream in = new ByteBufInputStream(buff.getByteBuf());
                ObjectReader reader = (ObjectReader) invoker.getBindingContext().get(ObjectReader.class.getName());
                try {
                    input = reader.readValue((InputStream) in);
                } catch (Exception e) {
                    log.error("Failed to unmarshal input", e);
                    request.fail(400);
                    return;
                }
            }
            Object finalInput = input;
            executor.execute(() -> {
                dispatch(request, invoker, finalInput);
            });
        });
    }

    private void dispatch(RoutingContext routingContext, FunctionInvoker invoker, Object input) {
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
            routingContext.response().setStatusCode(200);
            if (invoker.hasOutput()) {
                routingContext.response().putHeader("Content-Type", "application/json");
                ObjectWriter writer = (ObjectWriter) invoker.getBindingContext().get(ObjectWriter.class.getName());
                CompletionStage<?> output = funqyResponse.getOutput();
                output.whenCompleteAsync((o, t) -> {
                    if (t != null) {
                        routingContext.fail(t);
                        return;
                    }
                    try {
                        routingContext.response().end(writer.writeValueAsString(o));
                    } catch (JsonProcessingException e) {
                        log.error("Failed to marshal", e);
                        routingContext.fail(400);
                    }
                }, executor);
            } else {
                routingContext.response().end();
            }
        } catch (Exception e) {
            routingContext.fail(e);
        } finally {
            if (requestContext.isActive()) {
                requestContext.terminate();
            }
        }
    }
}
