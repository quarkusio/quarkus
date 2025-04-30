package io.quarkus.resteasy.reactive.server.runtime.websocket;

import jakarta.ws.rs.container.CompletionCallback;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;

import io.quarkus.resteasy.reactive.server.runtime.QuarkusResteasyReactiveRequestContext;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.ServerWebSocket;

//TODO: do we actually want vert.x websockets?
//they are not our primary API, but they are easy to support
public class VertxWebSocketParamExtractor implements ParameterExtractor {
    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        QuarkusResteasyReactiveRequestContext qrs = (QuarkusResteasyReactiveRequestContext) context;
        HttpServerRequest req = qrs.vertxServerRequest();
        ParameterCallback parameterCallback = new ParameterCallback();
        req.toWebSocket(new Handler<AsyncResult<ServerWebSocket>>() {
            @Override
            public void handle(AsyncResult<ServerWebSocket> event) {
                if (event.succeeded()) {
                    ServerWebSocket result = event.result();
                    result.closeHandler(new Handler<Void>() {
                        @Override
                        public void handle(Void event) {
                            context.close();
                        }
                    });
                    parameterCallback.setResult(result);
                    //the completion callback is generally invoked by the websocket close
                    //so the close will be a no-op
                    //if there is an exception somewhere else in processing though
                    //we want to close the websocket
                    context.registerCompletionCallback(new CompletionCallback() {
                        @Override
                        public void onComplete(Throwable throwable) {
                            result.close();
                        }
                    });
                } else {
                    parameterCallback.setFailure(event.cause());
                }

            }
        });
        return parameterCallback;
    }
}
