package io.quarkus.funqy.runtime.bindings.http;

import java.io.InputStream;
import java.util.concurrent.Executor;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

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
import io.quarkus.funqy.runtime.query.QueryReader;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
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
        // make sure rootPath ends with "/" for easy parsing
        if (rootPath == null) {
            this.rootPath = "/";
        } else if (!rootPath.endsWith("/")) {
            this.rootPath = rootPath + "/";
        } else {
            this.rootPath = rootPath;
        }

        this.executor = executor;
        Instance<CurrentIdentityAssociation> association = CDI.current().select(CurrentIdentityAssociation.class);
        this.association = association.isResolvable() ? association.get() : null;
        currentVertxRequest = CDI.current().select(CurrentVertxRequest.class).get();
    }

    @Override
    public void handle(RoutingContext routingContext) {
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

        FunctionInvoker invoker = FunctionRecorder.registry.matchInvoker(path);

        if (invoker == null) {
            routingContext.fail(404);
            return;
        }

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
            Object finalInput = input;
            executor.execute(() -> {
                dispatch(routingContext, invoker, finalInput);
            });
        } else if (routingContext.request().method() == HttpMethod.POST) {
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
                    dispatch(routingContext, invoker, finalInput);
                });
            });
        } else {
            routingContext.fail(405);
            log.error("Must be POST or GET for: " + invoker.getName());
        }
    }

    private void dispatch(RoutingContext routingContext, FunctionInvoker invoker, Object input) {
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
            FunqyRequestImpl funqyRequest = new FunqyRequestImpl(new RequestContextImpl(), input);
            FunqyResponseImpl funqyResponse = new FunqyResponseImpl();
            invoker.invoke(funqyRequest, funqyResponse);

            funqyResponse.getOutput().emitOn(executor).subscribe().with(
                    o -> {
                        if (invoker.hasOutput()) {
                            routingContext.response().setStatusCode(200);
                            routingContext.response().putHeader("Content-Type", "application/json");
                            ObjectWriter writer = (ObjectWriter) invoker.getBindingContext().get(ObjectWriter.class.getName());
                            try {
                                routingContext.response().end(writer.writeValueAsString(o));
                            } catch (JsonProcessingException e) {
                                log.error("Failed to marshal", e);
                                routingContext.fail(400);
                            }
                        } else {
                            routingContext.response().setStatusCode(204);
                            routingContext.response().end();
                        }
                    },
                    t -> routingContext.fail(t));

        } catch (Exception e) {
            routingContext.fail(e);
        } finally {
            if (requestContext.isActive()) {
                requestContext.terminate();
            }
        }
    }
}
