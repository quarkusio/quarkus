package io.quarkus.resteasy.runtime.standalone;

import java.io.IOException;

import org.jboss.logging.Logger;
import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.core.ThreadLocalResteasyProviderFactory;
import org.jboss.resteasy.plugins.server.embedded.SecurityDomain;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

/**
 * Helper/delegate class to unify Servlet and Filter dispatcher implementations
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author Norman Maurer
 * @version $Revision: 1 $
 */
public class RequestDispatcher {
    private static final Logger log = Logger.getLogger("io.quarkus.resteasy");

    protected final SynchronousDispatcher dispatcher;
    protected final ResteasyProviderFactory providerFactory;
    protected final SecurityDomain domain;
    protected final ClassLoader classLoader;

    public RequestDispatcher(final SynchronousDispatcher dispatcher, final ResteasyProviderFactory providerFactory,
            final SecurityDomain domain, ClassLoader classLoader) {
        this.dispatcher = dispatcher;
        this.providerFactory = providerFactory;
        this.domain = domain;
        this.classLoader = classLoader;
    }

    public SynchronousDispatcher getDispatcher() {
        return dispatcher;
    }

    public SecurityDomain getDomain() {
        return domain;
    }

    public ResteasyProviderFactory getProviderFactory() {
        return providerFactory;
    }

    public void service(Context context,
            HttpServerRequest req,
            HttpServerResponse resp,
            HttpRequest vertxReq, HttpResponse vertxResp, boolean handleNotFound) throws IOException {

        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            ResteasyProviderFactory defaultInstance = ResteasyProviderFactory.getInstance();
            if (defaultInstance instanceof ThreadLocalResteasyProviderFactory) {
                ThreadLocalResteasyProviderFactory.push(providerFactory);
            }

            try {
                ResteasyContext.pushContext(Context.class, context);
                ResteasyContext.pushContext(HttpServerRequest.class, req);
                ResteasyContext.pushContext(HttpServerResponse.class, resp);
                ResteasyContext.pushContext(Vertx.class, context.owner());
                if (handleNotFound) {
                    dispatcher.invoke(vertxReq, vertxResp);
                } else {
                    dispatcher.invokePropagateNotFound(vertxReq, vertxResp);
                }
            } finally {
                ResteasyContext.clearContextData();
            }
        } finally {
            try {
                ResteasyProviderFactory defaultInstance = ResteasyProviderFactory.getInstance();
                if (defaultInstance instanceof ThreadLocalResteasyProviderFactory) {
                    ThreadLocalResteasyProviderFactory.pop();
                }
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }

        }
    }
}
