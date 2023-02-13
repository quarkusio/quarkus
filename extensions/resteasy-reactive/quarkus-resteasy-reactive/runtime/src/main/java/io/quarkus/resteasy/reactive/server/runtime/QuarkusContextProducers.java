package io.quarkus.resteasy.reactive.server.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Providers;

import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.jboss.resteasy.reactive.server.jaxrs.ProvidersImpl;

import io.vertx.core.http.HttpServerResponse;

/**
 * Provides CDI producers for objects that can be injected via @Context
 * In RESTEasy Reactive this works because @Context is considered an alias for @Inject
 * through the use of {@code AutoInjectAnnotationBuildItem}
 */
@Singleton
public class QuarkusContextProducers {

    @RequestScoped
    @Produces
    HttpServerResponse httpServerResponse() {
        return CurrentRequestManager.get().serverRequest().unwrap(HttpServerResponse.class);
    }

    @ApplicationScoped
    @Produces
    Providers providers() {
        return new ProvidersImpl(ResteasyReactiveRecorder.getCurrentDeployment());
    }
}
