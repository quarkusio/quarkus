package io.quarkus.resteasy.runtime.standalone;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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

import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.security.identity.CurrentIdentityAssociation;
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
    protected final BeanContainer beanContainer;
    protected final CurrentIdentityAssociation association;
    protected final CurrentVertxRequest currentVertxRequest;
    protected final Executor executor;

    public VertxRequestHandler(Vertx vertx,
            BeanContainer beanContainer,
            ResteasyDeployment deployment,
            String rootPath,
            BufferAllocator allocator, Executor executor) {
        this.vertx = vertx;
        this.beanContainer = beanContainer;
        this.dispatcher = new RequestDispatcher((SynchronousDispatcher) deployment.getDispatcher(),
                deployment.getProviderFactory(), null, Thread.currentThread().getContextClassLoader());
        this.rootPath = rootPath;
        this.allocator = allocator;
        this.executor = executor;
        Instance<CurrentIdentityAssociation> association = CDI.current().select(CurrentIdentityAssociation.class);
        this.association = association.isResolvable() ? association.get() : null;
        currentVertxRequest = CDI.current().select(CurrentVertxRequest.class).get();
    }

    @Override
    public void handle(RoutingContext request) {
        // have to create input stream here.  Cannot execute in another thread
        // otherwise request handlers may not get set up before request ends
        InputStream is;
        try {
            if (request.getBody() != null) {
                is = new ByteArrayInputStream(request.getBody().getBytes());
            } else {
                is = new VertxInputStream(request);
            }
        } catch (IOException e) {
            request.fail(e);
            return;
        }

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

    private void dispatch(RoutingContext routingContext, InputStream is, VertxOutput output) {
        ManagedContext requestContext = beanContainer.requestContext();
        requestContext.activate();
        QuarkusHttpUser user = (QuarkusHttpUser) routingContext.user();
        if (user != null && association != null) {
            association.setIdentity(user.getSecurityIdentity());
        }
        currentVertxRequest.setCurrent(routingContext);
        try {
            Context ctx = vertx.getOrCreateContext();
            HttpServerRequest request = routingContext.request();
            ResteasyUriInfo uriInfo = VertxUtil.extractUriInfo(request, rootPath);
            ResteasyHttpHeaders headers = VertxUtil.extractHttpHeaders(request);
            HttpServerResponse response = request.response();
            VertxHttpResponse vertxResponse = new VertxHttpResponse(request, dispatcher.getProviderFactory(),
                    request.method(), allocator, output);

            // using a supplier to make the remote Address resolution lazy: often it's not needed and it's not very cheap to create.
            LazyHostSupplier hostSupplier = new LazyHostSupplier(request);

            VertxHttpRequest vertxRequest = new VertxHttpRequest(ctx, routingContext, headers, uriInfo, request.rawMethod(),
                    hostSupplier,
                    dispatcher.getDispatcher(), vertxResponse, requestContext);
            vertxRequest.setInputStream(is);
            try {
                ResteasyContext.pushContext(SecurityContext.class, new QuarkusResteasySecurityContext(request));
                ResteasyContext.pushContext(RoutingContext.class, routingContext);
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
