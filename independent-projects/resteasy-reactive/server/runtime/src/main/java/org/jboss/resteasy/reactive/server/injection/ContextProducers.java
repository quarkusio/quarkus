package org.jboss.resteasy.reactive.server.injection;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.sse.Sse;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;
import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.jaxrs.ResourceContextImpl;
import org.jboss.resteasy.reactive.server.jaxrs.SseImpl;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

/**
 * Provides CDI producers for objects that can be injected via @Context
 * In quarkus-rest this works because @Context is considered an alias for @Inject
 * through the use of {@code AutoInjectAnnotationBuildItem}
 */
@Singleton
public class ContextProducers {

    // NOTE: Same list for parameters in ContextParamExtractor
    // and in EndpointIndexer.CONTEXT_TYPES

    @RequestScoped
    @Produces
    ServerRequestContext quarkusRestContext() {
        return getContext();
    }

    @RequestScoped
    @Produces
    UriInfo uriInfo() {
        return getContext().getUriInfo();
    }

    @RequestScoped
    @Produces
    HttpHeaders headers() {
        return getContext().getHttpHeaders();
    }

    @ApplicationScoped
    @Produces
    Sse sse() {
        return SseImpl.INSTANCE;
    }

    @RequestScoped
    @Produces
    Request request() {
        return getContext().getRequest();
    }

    // HttpServerRequest, HttpServerRequest are Vert.x types so it's not necessary to have it injectable via @Context,
    // however we do use it in the Quickstarts so let's make it work

    //    @RequestScoped
    //    @Produces
    //    HttpServerRequest httpServerRequest() {
    //        return CurrentRequest.get().getContext().request();
    //    }

    //    @RequestScoped
    //    @Produces
    //    HttpServerResponse httpServerResponse() {
    //        return CurrentRequest.get().getContext().response();
    //    }

    @ApplicationScoped
    @Produces
    Providers providers() {
        return getContext().getProviders();
    }

    @RequestScoped
    @Produces
    ResourceInfo resourceInfo() {
        RuntimeResource target = getTarget();
        if (target != null) {
            return target.getLazyMethod();
        }
        return SimpleResourceInfo.NullValues.INSTANCE;
    }

    @RequestScoped
    @Produces
    SimpleResourceInfo simplifiedResourceInfo() {
        RuntimeResource target = getTarget();
        if (target != null) {
            return target.getSimplifiedResourceInfo();
        }
        return SimpleResourceInfo.NullValues.INSTANCE;
    }

    private RuntimeResource getTarget() {
        return getContext().getTarget();
    }

    @ApplicationScoped
    @Produces
    Configuration config() {
        return getContext().getDeployment().getConfiguration();
    }

    @ApplicationScoped
    @Produces
    Application application() {
        return getContext().getDeployment().getApplicationSupplier().get();
    }

    @ApplicationScoped
    @Produces
    ResourceContext resourceContext() {
        return ResourceContextImpl.INSTANCE;
    }

    @RequestScoped
    @Produces
    SecurityContext securityContext() {
        return getContext().getSecurityContext();
    }

    private ResteasyReactiveRequestContext getContext() {
        return CurrentRequestManager.get();
    }
}
