package io.quarkus.it.kafka;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.quarkus.logging.Log;
import io.quarkus.test.vertx.VirtualThreadsAssertions;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

@ApplicationScoped
public class FlowersContextualReceivers {

    @Inject
    RequestBean reqBean;

    @Inject
    Logger logger;

    @Incoming("contextual-flower")
    void processContextual(String name) {
        Context ctx = Vertx.currentContext();
        Log.info(ctx + "[" + ctx.getClass() + "]");
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        logger.infof("Hello, %s", reqBean.getName());
    }

    @Blocking
    @Incoming("contextual-flower-blocking")
    void processContextualBlocking(String name) {
        Context ctx = Vertx.currentContext();
        assert Context.isOnWorkerThread();
        Log.info(ctx + "[" + ctx.getClass() + "]");
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        logger.infof("Hello, %s", reqBean.getName());
    }

    @Blocking("named-pool")
    @Incoming("contextual-flower-blocking-named")
    void processContextualBlockingNamed(String name) {
        Context ctx = Vertx.currentContext();
        assert Context.isOnWorkerThread();
        Log.info(ctx + "[" + ctx.getClass() + "]");
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        logger.infof("Hello, %s", reqBean.getName());
    }

    @RunOnVirtualThread
    @Incoming("contextual-flower-virtual-thread")
    void processContextualVT(String name) {
        Context ctx = Vertx.currentContext();
        VirtualThreadsAssertions.assertThatItRunsOnVirtualThread();
        VirtualThreadsAssertions.assertThatItRunsOnADuplicatedContext();
        Log.info(ctx + "[" + ctx.getClass() + "]");
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        logger.infof("Hello, %s", reqBean.getName());
    }
}
