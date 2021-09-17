package io.quarkus.resteasy.runtime.standalone;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.core.SecurityContext;

import org.jboss.logging.Logger;
import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jboss.resteasy.specimpl.ResteasyUriInfo;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.ResteasyDeployment;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.resteasy.runtime.ContextUtil;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.VertxInputStream;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxRequestHandler implements Handler<RoutingContext> {
    private static final Logger log = Logger.getLogger("io.quarkus.resteasy");

    protected final Vertx vertx;
    protected final RequestDispatcher dispatcher;
    protected final String rootPath;
    protected final BufferAllocator allocator;
    protected final CurrentIdentityAssociation association;
    protected final CurrentVertxRequest currentVertxRequest;
    protected final Executor executor;
    protected final long readTimeout;

    public VertxRequestHandler(Vertx vertx,
            ResteasyDeployment deployment,
            String rootPath,
            BufferAllocator allocator, Executor executor, long readTimeout) {
        this.vertx = vertx;
        this.dispatcher = new RequestDispatcher((SynchronousDispatcher) deployment.getDispatcher(),
                deployment.getProviderFactory(), null, Thread.currentThread().getContextClassLoader());
        this.rootPath = rootPath;
        this.allocator = allocator;
        this.executor = executor;
        this.readTimeout = readTimeout;
        Instance<CurrentIdentityAssociation> association = CDI.current().select(CurrentIdentityAssociation.class);
        this.association = association.isResolvable() ? association.get() : null;
        currentVertxRequest = CDI.current().select(CurrentVertxRequest.class).get();
    }

    @Override
    public void handle(RoutingContext request) {
        // have to create input stream here.  Cannot execute in another thread
        // otherwise request handlers may not get set up before request ends
        InputStream is;
        if (request.getBody() != null) {
            is = new ByteArrayInputStream(request.getBody().getBytes());
        } else {
            is = new VertxInputStream(request, readTimeout);
        }
        if (BlockingOperationControl.isBlockingAllowed()) {
            try {
                dispatch(request, is, new VertxBlockingOutput(request.request()));
            } catch (Throwable e) {
                request.fail(e);
            }
        } else {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        dispatch(request, is, new VertxBlockingOutput(request.request()));
                    } catch (Throwable e) {
                        request.fail(e);
                    }
                }
            });
        }

    }

    private void dispatch(RoutingContext routingContext, InputStream is, VertxOutput output) {
        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        routingContext.remove(QuarkusHttpUser.AUTH_FAILURE_HANDLER);
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
            Context ctx = vertx.getOrCreateContext();
            HttpServerRequest request = routingContext.request();
            ResteasyUriInfo uriInfo = VertxUtil.extractUriInfo(request, rootPath);
            ResteasyHttpHeaders headers = VertxUtil.extractHttpHeaders(request);
            HttpServerResponse response = request.response();
            VertxHttpResponse vertxResponse = new VertxHttpResponse(request, dispatcher.getProviderFactory(),
                    request.method(), allocator, output, routingContext);

            // using a supplier to make the remote Address resolution lazy: often it's not needed and it's not very cheap to create.
            LazyHostSupplier hostSupplier = new LazyHostSupplier(request);

            VertxHttpRequest vertxRequest = new VertxHttpRequest(ctx, routingContext, headers, uriInfo, request.method().name(),
                    hostSupplier,
                    dispatcher.getDispatcher(), vertxResponse, requestContext, executor);
            vertxRequest.setInputStream(is);
            Map<Class<?>, Object> map = new HashMap<>();
            map.put(SecurityContext.class, new QuarkusResteasySecurityContext(request, routingContext));
            map.put(RoutingContext.class, routingContext);
            try (ResteasyContext.CloseableContext restCtx = ResteasyContext.addCloseableContextDataLevel(map)) {
                ContextUtil.pushContext(routingContext);
                dispatcher.service(ctx, request, response, vertxRequest, vertxResponse, true);
            } catch (Failure e1) {
                vertxResponse.setStatus(e1.getErrorCode());
                if (e1.isLoggable()) {
                    log.error(e1);
                }
            } catch (Throwable ex) {
                routingContext.fail(ex);
            }

            boolean suspended = vertxRequest.getAsyncContext().isSuspended();
            boolean requestContextActive = requestContext.isActive();
            if (!suspended) {
                try {
                    if (requestContextActive) {
                        requestContext.terminate();
                    }
                } finally {
                    try {
                        vertxResponse.finish();
                    } catch (IOException e) {
                        log.debug("IOException writing JAX-RS response", e);
                    }
                }
            } else {
                //we need the request context to stick around
                requestContext.deactivate();
            }
        } catch (Throwable t) {
            try {
                routingContext.fail(t);
            } finally {
                if (requestContext.isActive()) {
                    requestContext.terminate();
                }
            }
        }
    }

}
