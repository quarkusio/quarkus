package org.jboss.resteasy.reactive.server.handlers;

import java.lang.reflect.Constructor;
import java.util.concurrent.*;

import org.jboss.resteasy.reactive.server.core.BlockingOperationSupport;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

//should not be used anymore, but might be in the future if using an event-loop as a carrier doesn't cause deadlocks anymore
public class VirtualThreadNonBlockingHandler implements ServerRestHandler {
    private Executor executor;
    private static volatile ConcurrentHashMap<String, Executor> eventLoops = new ConcurrentHashMap<>();

    public VirtualThreadNonBlockingHandler() {
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        if (BlockingOperationSupport.isBlockingAllowed()) {
            return; // already dispatched
        }

        if (!eventLoops.containsKey(Thread.currentThread().toString())) {
            var vtf = Class.forName("java.lang.ThreadBuilders").getDeclaredClasses()[0];
            Constructor constructor = vtf.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            ThreadFactory tf = (ThreadFactory) constructor.newInstance(
                    new Object[] { requestContext.getContextExecutor(), "quarkus-virtual-factory", 0, 0, null });
            var exec = (Executor) Executors.class.getMethod("newThreadPerTaskExecutor", ThreadFactory.class)
                    .invoke(this, tf);
            eventLoops.put(Thread.currentThread().toString(), exec);
        }
        requestContext.suspend();
        requestContext.resume(eventLoops.get(Thread.currentThread().toString()));
    }
}
