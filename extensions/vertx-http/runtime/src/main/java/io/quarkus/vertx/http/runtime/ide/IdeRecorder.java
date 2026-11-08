package io.quarkus.vertx.http.runtime.ide;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.dev.console.DevConsoleManager;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class IdeRecorder {

    private IdeRecorder() {
    }

    public static Handler<RoutingContext> openInIde() {
        return new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext rc) {
                DevConsoleManager.invoke("devui-ide-interaction.open", rc.pathParams());
                rc.response()
                        .setStatusCode(HttpResponseStatus.ACCEPTED.code()).end();
            }
        };
    }

}
