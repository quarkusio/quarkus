package io.quarkus.resteasy.runtime.standalone;

import java.io.IOException;
import java.io.InputStream;

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
    protected final String servletMappingPrefix;
    protected final BufferAllocator allocator;
    protected final BeanContainer beanContainer;
    protected final CurrentIdentityAssociation association;

    public VertxRequestHandler(Vertx vertx,
            BeanContainer beanContainer,
            ResteasyDeployment deployment,
            String servletMappingPrefix,
            BufferAllocator allocator) {
        this.vertx = vertx;
        this.beanContainer = beanContainer;
        this.dispatcher = new RequestDispatcher((SynchronousDispatcher) deployment.getDispatcher(),
                deployment.getProviderFactory(), null);
        this.servletMappingPrefix = servletMappingPrefix;
        this.allocator = allocator;
        Instance<CurrentIdentityAssociation> association = CDI.current().select(CurrentIdentityAssociation.class);
        this.association = association.isResolvable() ? association.get() : null;
    }

    @Override
    public void handle(RoutingContext request) {
        // have to create input stream here.  Cannot execute in another thread
        // otherwise request handlers may not get set up before request ends
        VertxInputStream is;
        try {
            is = new VertxInputStream(request.request());
        } catch (IOException e) {
            request.fail(e);
            return;
        }

        vertx.executeBlocking(event -> {
            dispatchRequestContext(request, is, new VertxBlockingOutput(request.request()));
        }, false, event -> {
        });
    }

    private void dispatchRequestContext(RoutingContext request, InputStream is, VertxOutput output) {
        ManagedContext requestContext = beanContainer.requestContext();
        requestContext.activate();
        QuarkusHttpUser user = (QuarkusHttpUser) request.user();
        if (user != null && association != null) {
            association.setIdentity(user.getSecurityIdentity());
        }
        try {
            dispatch(request, is, output);
        } finally {
            requestContext.terminate();
        }
    }

    private void dispatch(RoutingContext routingContext, InputStream is, VertxOutput output) {
        try {
            Context ctx = vertx.getOrCreateContext();
            HttpServerRequest request = routingContext.request();
            ResteasyUriInfo uriInfo = VertxUtil.extractUriInfo(request, servletMappingPrefix);
            ResteasyHttpHeaders headers = VertxUtil.extractHttpHeaders(request);
            HttpServerResponse response = request.response();
            VertxHttpResponse vertxResponse = new VertxHttpResponse(request, dispatcher.getProviderFactory(),
                    request.method(), allocator, output);
            VertxHttpRequest vertxRequest = new VertxHttpRequest(ctx, headers, uriInfo, request.rawMethod(),
                    dispatcher.getDispatcher(), vertxResponse, false);
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

            if (!vertxRequest.getAsyncContext().isSuspended()) {
                try {
                    vertxResponse.finish();
                } catch (IOException e) {
                    log.error("Unexpected failure", e);
                }
            }
        } catch (Throwable t) {
            routingContext.fail(t);
        }
    }
}
