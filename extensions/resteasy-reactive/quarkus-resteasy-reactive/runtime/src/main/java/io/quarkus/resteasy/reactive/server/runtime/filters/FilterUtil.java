package io.quarkus.resteasy.reactive.server.runtime.filters;

import java.util.Optional;
import java.util.function.Consumer;

import javax.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;

import io.smallrye.mutiny.Uni;

/**
 * This class is used by the filters that are generated as a result of the use of {@link ServerRequestFilter} or
 * {@link ServerResponseFilter} on methods
 * that don't return {@code void}.
 * See {@code io.quarkus.resteasy.reactive.server.deployment.CustomFilterGenerator}
 */
public final class FilterUtil {

    private FilterUtil() {
    }

    public static void handleOptional(Optional<Response> optional,
            ResteasyReactiveContainerRequestContext context) {
        if ((optional != null) && optional.isPresent()) {
            context.abortWith(optional.get());
        }
    }

    public static void handleResponse(Response response,
            ResteasyReactiveContainerRequestContext context) {
        if (response != null) {
            context.abortWith(response);
        }
    }

    public static void handleUniVoid(Uni<Void> uni, ResteasyReactiveContainerRequestContext context) {
        if (uni == null) {
            return;
        }
        context.suspend();
        uni.subscribe().with(new Consumer<Void>() {
            @Override
            public void accept(Void unused) {
                context.resume();
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                context.resume(throwable);
            }
        });
    }

    public static void handleUniResponse(Uni<Response> uni, ResteasyReactiveContainerRequestContext context) {
        if (uni == null) {
            return;
        }
        context.suspend();
        uni.subscribe().with(new Consumer<Response>() {
            @Override
            public void accept(Response response) {
                if (response != null) {
                    context.abortWith(response);
                } else {
                    context.resume();
                }
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                context.resume(throwable);
            }
        });
    }
}
