package io.quarkus.resteasy.reactive.server.runtime.exceptionmappers;

import java.util.function.Consumer;
import java.util.function.Function;

import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.resteasy.reactive.server.spi.AsyncExceptionMapperContext;

import io.smallrye.mutiny.Uni;

/**
 * This class is used by the exception mappers that are generated as a result of the use of {@link ServerExceptionMapper}
 * with a {@link Uni} response type.
 * See {@code io.quarkus.resteasy.reactive.server.deployment.ServerExceptionMapperGenerator}
 */
public final class AsyncExceptionMappingUtil {

    private static final Logger log = Logger.getLogger(AsyncExceptionMappingUtil.class);

    private static final Response DEFAULT_RESPONSE = Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity("Internal Server Error").build();

    static final Response DEFAULT_UNAUTHORIZED_RESPONSE = Response.status(Response.Status.UNAUTHORIZED)
            .entity("Not Authenticated").build();

    private AsyncExceptionMappingUtil() {
    }

    public static void handleUniResponse(Uni<Response> asyncResponse, AsyncExceptionMapperContext context) {
        context.suspend();
        asyncResponse.subscribe().with(new Consumer<Response>() {
            @Override
            public void accept(Response response) {
                if (response == null) {
                    log.debug("Response was null, returning default error response");
                    context.setResponse(DEFAULT_RESPONSE);
                } else {
                    context.setResponse(response);
                }
                context.resume();
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                log.error("Unable to convert exception to Response", throwable);
                context.setResponse(DEFAULT_RESPONSE);
                context.resume();
            }
        });
    }

    public static void handleUniRestResponse(Uni<? extends RestResponse<?>> asyncResponse,
            AsyncExceptionMapperContext context) {
        handleUniResponse(asyncResponse.map(new Function<RestResponse<?>, Response>() {
            @Override
            public Response apply(RestResponse<?> t) {
                return t != null ? t.toResponse() : null;
            }
        }), context);
    }
}
