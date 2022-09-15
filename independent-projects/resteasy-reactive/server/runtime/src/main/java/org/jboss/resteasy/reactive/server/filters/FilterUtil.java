package org.jboss.resteasy.reactive.server.filters;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;

/**
 * This class is used by the filters that are generated as a result of the use of {@link ServerRequestFilter} or
 * {@link ServerResponseFilter} on methods
 * that don't return {@code void}.
 * See {@code io.quarkus.resteasy.reactive.server.deployment.CustomFilterGenerator}
 */
@SuppressWarnings("unused")
public final class FilterUtil {

    private FilterUtil() {
    }

    public static void handleOptional(Optional<Response> optional,
            ResteasyReactiveContainerRequestContext context) {
        if ((optional != null) && optional.isPresent()) {
            context.abortWith(optional.get());
        }
    }

    public static void handleOptionalRestResponse(Optional<RestResponse<?>> optional,
            ResteasyReactiveContainerRequestContext context) {
        if ((optional != null) && optional.isPresent()) {
            context.abortWith(optional.get().toResponse());
        }
    }

    public static void handleResponse(Response response,
            ResteasyReactiveContainerRequestContext context) {
        if (response != null) {
            context.abortWith(response);
        }
    }

    public static void handleRestResponse(RestResponse<?> response,
            ResteasyReactiveContainerRequestContext context) {
        if (response != null) {
            context.abortWith(response.toResponse());
        }
    }

    public static void handleUniVoid(Uni<Object> uni, ResteasyReactiveContainerRequestContext context) {
        if (uni == null) {
            return;
        }
        context.suspend();
        uni.subscribe().with(new Consumer<>() {
            @Override
            public void accept(Object o) {
                context.resume();
            }
        }, new ResumeThrowableConsumer(context));
    }

    public static void handleUniResponse(Uni<Response> uni, ResteasyReactiveContainerRequestContext context) {
        if (uni == null) {
            return;
        }
        context.suspend();
        uni.subscribe().with(new Consumer<>() {
            @Override
            public void accept(Response response) {
                if (response != null) {
                    context.abortWith(response);
                } else {
                    context.resume();
                }
            }
        }, new ResumeThrowableConsumer(context));
    }

    public static void handleUniRestResponse(Uni<? extends RestResponse<?>> uni,
            ResteasyReactiveContainerRequestContext context) {
        if (uni == null) {
            return;
        }
        handleUniResponse(uni.map(new Function<RestResponse<?>, Response>() {
            @Override
            public Response apply(RestResponse<?> t) {
                return t != null ? t.toResponse() : null;
            }
        }), context);
    }

    private static class ResumeThrowableConsumer implements Consumer<Throwable> {
        private final ResteasyReactiveContainerRequestContext context;

        public ResumeThrowableConsumer(ResteasyReactiveContainerRequestContext context) {
            this.context = context;
        }

        @Override
        public void accept(Throwable throwable) {
            context.resume(throwable);
        }
    }
}
