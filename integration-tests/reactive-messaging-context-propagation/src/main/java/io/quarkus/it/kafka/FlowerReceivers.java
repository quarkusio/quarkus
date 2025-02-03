package io.quarkus.it.kafka;

import java.util.Objects;

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
public class FlowerReceivers {

    @Inject
    RequestBean reqBean;

    @Inject
    Logger logger;

    @Incoming("flower")
    void process(String id) {
        Context ctx = Vertx.currentContext();
        assert Context.isOnEventLoopThread();
        Log.info(ctx.hashCode() + " " + ctx);
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        logger.infof("Greeting, %s", reqBean.getName());
        if (!Objects.equals(id, reqBean.getId())) {
            throw new IllegalStateException("RequestScoped context not propagated");
        }
    }

    @Blocking
    @Incoming("flower-blocking")
    void processBlocking(String id) {
        Context ctx = Vertx.currentContext();
        assert Context.isOnWorkerThread();
        Log.info(ctx.hashCode() + " " + ctx);
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        logger.infof("Greeting, %s", reqBean.getName());
        if (!Objects.equals(id, reqBean.getId())) {
            throw new IllegalStateException("RequestScoped context not propagated");
        }
    }

    @Blocking("named-pool")
    @Incoming("flower-blocking-named")
    void processBlockingNamed(String id) {
        Context ctx = Vertx.currentContext();
        assert Context.isOnWorkerThread();
        Log.info(ctx.hashCode() + " " + ctx);
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        logger.infof("Greeting, %s", reqBean.getName());
        if (!Objects.equals(id, reqBean.getId())) {
            throw new IllegalStateException("RequestScoped context not propagated");
        }
    }

    @RunOnVirtualThread
    @Incoming("flower-virtual-thread")
    void processVT(String id) {
        Context ctx = Vertx.currentContext();
        VirtualThreadsAssertions.assertThatItRunsOnVirtualThread();
        VirtualThreadsAssertions.assertThatItRunsOnADuplicatedContext();
        Log.info(ctx.hashCode() + " " + ctx);
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        logger.infof("Greeting, %s", reqBean.getName());
        if (!Objects.equals(id, reqBean.getId())) {
            throw new IllegalStateException("RequestScoped context not propagated");
        }
    }

}
