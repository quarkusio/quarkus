package io.quarkus.it.kafka;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.quarkus.logging.Log;
import io.quarkus.test.vertx.VirtualThreadsAssertions;
import io.smallrye.common.annotation.NonBlocking;
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
    void process(String name) {
        Context ctx = Vertx.currentContext();
        assert Context.isOnEventLoopThread();
        Log.info(ctx + "[" + ctx.getClass() + "]");
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        logger.infof("Greeting, %s", reqBean.getName());
    }

    @Blocking
    @Incoming("flower-blocking")
    void processBlocking(String name) {
        Context ctx = Vertx.currentContext();
        assert Context.isOnWorkerThread();
        Log.info(ctx + "[" + ctx.getClass() + "]");
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        logger.infof("Greeting, %s", reqBean.getName());
    }

    @Blocking("named-pool")
    @Incoming("flower-blocking-named")
    void processBlockingNamed(String name) {
        Context ctx = Vertx.currentContext();
        assert Context.isOnWorkerThread();
        Log.info(ctx + "[" + ctx.getClass() + "]");
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        logger.infof("Greeting, %s", reqBean.getName());
    }

    @RunOnVirtualThread
    @Incoming("flower-virtual-thread")
    void processVT(String name) {
        Context ctx = Vertx.currentContext();
        VirtualThreadsAssertions.assertThatItRunsOnVirtualThread();
        VirtualThreadsAssertions.assertThatItRunsOnADuplicatedContext();
        Log.info(ctx + "[" + ctx.getClass() + "]");
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        logger.infof("Greeting, %s", reqBean.getName());
    }

    @Inject
    FlowerProducer producer;

    @Incoming("flowers-in")
    @NonBlocking
    void receive(String flower) {
        Context ctx = Vertx.currentContext();
        assert Context.isOnEventLoopThread();
        //        assert Context.isOnWorkerThread();
        Log.info(ctx + "[" + ctx.getClass() + "]");
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        System.out.println("Received io: " + flower);
        producer.addReceived(flower);
    }

    @Incoming("flowers-in")
    @Blocking
    void receiveBlocking(String flower) {
        Context ctx = Vertx.currentContext();
        assert Context.isOnWorkerThread();
        Log.info(ctx + "[" + ctx.getClass() + "]");
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        System.out.println("Received blocking: " + flower);
        producer.addReceived(flower);
    }

    @Incoming("flowers-in")
    @RunOnVirtualThread
    void receiveVT(String flower) {
        Context ctx = Vertx.currentContext();
        VirtualThreadsAssertions.assertThatItRunsOnVirtualThread();
        VirtualThreadsAssertions.assertThatItRunsOnADuplicatedContext();
        Log.info(ctx + "[" + ctx.getClass() + "]");
        Log.infof("bean: %s, id: %s", reqBean, reqBean.getId());
        System.out.println("Received vt: " + flower);
        producer.addReceived(flower);
    }

}
