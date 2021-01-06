package io.quarkus.scheduler.runtime.devconsole;

import java.time.Instant;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.quarkus.devconsole.runtime.spi.FlashScopeUtil.FlashMessageStatus;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.Trigger;
import io.quarkus.scheduler.runtime.ScheduledInvoker;
import io.quarkus.scheduler.runtime.ScheduledMethodMetadata;
import io.quarkus.scheduler.runtime.SchedulerContext;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class SchedulerDevConsoleRecorder {

    private static final Logger LOG = Logger.getLogger(SchedulerDevConsoleRecorder.class);

    public Handler<RoutingContext> invokeHandler() {
        // the usual issue of Vert.x hanging on to the first TCCL and setting it on all its threads
        final ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
        return new DevConsolePostHandler() {
            @Override
            protected void handlePost(RoutingContext event, MultiMap form) throws Exception {
                String name = form.get("name");
                SchedulerContext context = Arc.container().instance(SchedulerContext.class).get();
                for (ScheduledMethodMetadata metadata : context.getScheduledMethods()) {
                    if (metadata.getMethodDescription().equals(name)) {
                        context.getExecutor().execute(new Runnable() {
                            @Override
                            public void run() {
                                final ClassLoader previousCl = Thread.currentThread().getContextClassLoader();
                                try {
                                    Thread.currentThread().setContextClassLoader(currentCl);
                                    ScheduledInvoker invoker = context
                                            .createInvoker(metadata.getInvokerClassName());
                                    invoker.invoke(new DevModeScheduledExecution());
                                } catch (Exception e) {
                                    LOG.error(
                                            "Unable to invoke a @Scheduled method: "
                                                    + metadata.getMethodDescription(),
                                            e);
                                } finally {
                                    Thread.currentThread().setContextClassLoader(previousCl);
                                }
                            }
                        });
                        flashMessage(event, "Action invoked");
                        return;
                    }
                }
                flashMessage(event, "Action not found: " + name, FlashMessageStatus.ERROR);
            }
        };
    }

    private static class DevModeScheduledExecution implements ScheduledExecution {

        private final Instant now;

        DevModeScheduledExecution() {
            super();
            this.now = Instant.now();
        }

        @Override
        public Trigger getTrigger() {
            return new Trigger() {

                @Override
                public String getId() {
                    return "dev-console";
                }

                @Override
                public Instant getNextFireTime() {
                    return null;
                }

                @Override
                public Instant getPreviousFireTime() {
                    return now;
                }
            };
        }

        @Override
        public Instant getFireTime() {
            return now;
        }

        @Override
        public Instant getScheduledFireTime() {
            return now;
        }

    }

}
