package io.quarkus.reactivemessaging.http.runtime;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class ReactiveHttpRecorder {

    public Handler<RoutingContext> createWebSocketHandler() {
        ReactiveWebSocketHandlerBean bean = Arc.container().instance(ReactiveWebSocketHandlerBean.class).get();
        return new ReactiveWebSocketHandler(bean);
    }

    public Handler<RoutingContext> createHttpHandler() {
        ReactiveHttpHandlerBean bean = Arc.container().instance(ReactiveHttpHandlerBean.class).get();
        return new ReactiveHttpHandler(bean);
    }
}
