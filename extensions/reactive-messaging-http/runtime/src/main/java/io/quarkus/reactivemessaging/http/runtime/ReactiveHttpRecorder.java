package io.quarkus.reactivemessaging.http.runtime;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 01/10/2019
 */
@Recorder
public class ReactiveHttpRecorder {

    public Handler<RoutingContext> createWebsocketHandler() {
        ReactiveHttpHandlerBean bean = Arc.container().instance(ReactiveHttpHandlerBean.class).get();
        return new ReactiveWebsocketHandler(bean);
    }

    public Handler<RoutingContext> createHttpHandler() {
        ReactiveHttpHandlerBean bean = Arc.container().instance(ReactiveHttpHandlerBean.class).get();
        return new ReactiveHttpHandler(bean);
    }

    public Handler<RoutingContext> createBodyHandler() {
        return BodyHandler.create();
    }
}
