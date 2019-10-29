package io.quarkus.resteasy.runtime.standalone;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    protected final String rootPath;
    protected final int resteasyUriInfoCacheMaxSize;
    protected final BufferAllocator allocator;
    protected final BeanContainer beanContainer;
    protected final CurrentIdentityAssociation association;

    private final Map<String, ResteasyUriInfo.InitData> resteasyUriInfoInitDataMap = new ConcurrentHashMap<>();

    public VertxRequestHandler(Vertx vertx,
            BeanContainer beanContainer,
            ResteasyDeployment deployment,
            String rootPath,
            int resteasyUriInfoCacheMaxSize,
            BufferAllocator allocator) {
        this.vertx = vertx;
        this.beanContainer = beanContainer;
        this.dispatcher = new RequestDispatcher((SynchronousDispatcher) deployment.getDispatcher(),
                deployment.getProviderFactory(), null);
        this.rootPath = rootPath;
        this.resteasyUriInfoCacheMaxSize = resteasyUriInfoCacheMaxSize;
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

            String uriString = VertxUtil.getUriString(request);
            ResteasyUriInfo.InitData resteasyUriInfoCachedInitData = null;
            boolean setInitDataUponSuccess = false;
            if (ResteasyUriInfo.InitData.canBeCached(uriString) && resteasyUriInfoCacheMaxSize > 0) {
                String cacheKey = ResteasyUriInfo.InitData.getCacheKey(uriString, rootPath);
                resteasyUriInfoCachedInitData = resteasyUriInfoInitDataMap.get(cacheKey);
                if (resteasyUriInfoCachedInitData == null) {
                    setInitDataUponSuccess = true;
                }
            }
            if (resteasyUriInfoCachedInitData == null) {
                resteasyUriInfoCachedInitData = new ResteasyUriInfo.InitData(uriString, rootPath);
            }

            ResteasyUriInfo uriInfo = new ResteasyUriInfo(uriString, rootPath, resteasyUriInfoCachedInitData);

            ResteasyHttpHeaders headers = VertxUtil.extractHttpHeaders(request);
            HttpServerResponse response = request.response();
            VertxHttpResponse vertxResponse = new VertxHttpResponse(request, dispatcher.getProviderFactory(),
                    request.method(), allocator, output);
            VertxHttpRequest vertxRequest = new VertxHttpRequest(ctx, headers, uriInfo, request.rawMethod(),
                    request.remoteAddress().host(), dispatcher.getDispatcher(), vertxResponse, false);
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
                    // the reason we only cache successful responses is to ensure that a torrent of erroneous URLs
                    // doesn't fill up the cache and cause a DoS
                    if (setInitDataUponSuccess && vertxResponse.getStatus() >= 200 && vertxResponse.getStatus() <= 300) {
                        // we don't want the cache to grow unbounded since values path params could potentially be infinite
                        // and if left unchecked would take up all memory if the application is up for long enough
                        if (resteasyUriInfoInitDataMap.size() > resteasyUriInfoCacheMaxSize) {
                            resteasyUriInfoInitDataMap.clear(); // this is super lame and should probably be revisited
                        }
                        // this could potentially be written multiple times for initial requests but it doesn't matter
                        // since the data is always the same
                        resteasyUriInfoInitDataMap.put(ResteasyUriInfo.InitData.getCacheKey(uriString, rootPath),
                                resteasyUriInfoCachedInitData);
                    }
                } catch (IOException e) {
                    log.error("Unexpected failure", e);
                }
            }
        } catch (Throwable t) {
            routingContext.fail(t);
        }
    }
}
