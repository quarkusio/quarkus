package io.quarkus.it.kafka.contextual;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.quarkus.it.kafka.RequestBean;
import io.quarkus.logging.Log;
import io.quarkus.test.vertx.VirtualThreadsAssertions;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

@ApplicationScoped
public class FlowersMutinyReceivers {

    @Inject
    RequestBean reqBean;

    @Inject
    Logger logger;

    @Incoming("mutiny-flower")
    void processContextual(String id) {
        Context ctx = Vertx.currentContext();
        Log.info(ctx + "[" + ctx.getClass() + "]");
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        logger.infof("Hello, %s", reqBean.getName());
        assert Objects.equals(id, reqBean.getId());
    }

    @Blocking
    @Incoming("mutiny-flower-blocking")
    void processContextualBlocking(String id) {
        Context ctx = Vertx.currentContext();
        assert Context.isOnWorkerThread();
        Log.info(ctx + "[" + ctx.getClass() + "]");
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        logger.infof("Hello, %s", reqBean.getName());
        assert Objects.equals(id, reqBean.getId());
    }

    @Blocking("named-pool")
    @Incoming("mutiny-flower-blocking-named")
    void processContextualBlockingNamed(String id) {
        Context ctx = Vertx.currentContext();
        assert Context.isOnWorkerThread();
        Log.info(ctx + "[" + ctx.getClass() + "]");
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        logger.infof("Hello, %s", reqBean.getName());
        assert Objects.equals(id, reqBean.getId());
    }

    @RunOnVirtualThread
    @Incoming("mutiny-flower-virtual-thread")
    void processContextualVT(String id) {
        Context ctx = Vertx.currentContext();
        VirtualThreadsAssertions.assertThatItRunsOnVirtualThread();
        VirtualThreadsAssertions.assertThatItRunsOnADuplicatedContext();
        Log.info(ctx + "[" + ctx.getClass() + "]");
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        logger.infof("Hello, %s", reqBean.getName());
        assert Objects.equals(id, reqBean.getId());
    }
}
