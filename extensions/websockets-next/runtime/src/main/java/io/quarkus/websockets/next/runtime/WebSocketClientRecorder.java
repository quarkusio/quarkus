package io.quarkus.websockets.next.runtime;

import java.util.Map;
import java.util.function.Supplier;

import io.quarkus.runtime.annotations.RecordableConstructor;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.websockets.next.WebSocketClientException;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

@Recorder
public class WebSocketClientRecorder {

    public Supplier<Object> connectionSupplier() {
        return new Supplier<Object>() {

            @Override
            public Object get() {
                Context context = Vertx.currentContext();
                if (context != null && VertxContext.isDuplicatedContext(context)) {
                    Object connection = context.getLocal(ContextSupport.WEB_SOCKET_CONN_KEY);
                    if (connection != null) {
                        return connection;
                    }
                }
                throw new WebSocketClientException("Unable to obtain the connection from the Vert.x duplicated context");
            }
        };
    }

    public Supplier<Object> createContext(Map<String, ClientEndpoint> endpointMap) {
        return new Supplier<Object>() {
            @Override
            public Object get() {
                return new ClientEndpointsContext() {

                    @Override
                    public ClientEndpoint endpoint(String endpointClass) {
                        return endpointMap.get(endpointClass);
                    }

                };
            }
        };
    }

    public interface ClientEndpointsContext {

        ClientEndpoint endpoint(String endpointClass);

    }

    public static class ClientEndpoint {

        public final String clientId;

        public final String path;

        public final String generatedEndpointClass;

        @RecordableConstructor
        public ClientEndpoint(String clientId, String path, String generatedEndpointClass) {
            this.clientId = clientId;
            this.path = path;
            this.generatedEndpointClass = generatedEndpointClass;
        }

    }

}
