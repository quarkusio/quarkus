package io.quarkus.scheduler.runtime.devconsole;

import java.time.Instant;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.quarkus.devconsole.runtime.spi.FlashScopeUtil.FlashMessageStatus;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.Trigger;
import io.quarkus.scheduler.common.runtime.ScheduledInvoker;
import io.quarkus.scheduler.common.runtime.ScheduledMethod;
import io.quarkus.scheduler.common.runtime.SchedulerContext;
import io.quarkus.scheduler.common.runtime.util.SchedulerUtils;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class SchedulerDevConsoleRecorder {

    private static final Logger LOG = Logger.getLogger(SchedulerDevConsoleRecorder.class);

    public Supplier<Function<String, String>> getConfigLookup() {
        return new Supplier<Function<String, String>>() {

            @Override
            public Function<String, String> get() {
                return SchedulerUtils::lookUpPropertyValue;
            }
        };
    }

    public Handler<RoutingContext> invokeHandler() {
        // the usual issue of Vert.x hanging on to the first TCCL and setting it on all its threads
        final ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
        return new DevConsolePostHandler() {
            @Override
            protected void handlePost(RoutingContext ctx, MultiMap form) throws Exception {
                String action = form.get("action");
                if ("pause".equals(action)) {
                    Scheduler scheduler = Arc.container().instance(Scheduler.class).get();
                    if (scheduler.isRunning()) {
                        scheduler.pause();
                        LOG.info("Scheduler paused via Dev UI");
                        flashMessage(ctx, "Scheduler paused");
                    }
                } else if ("resume".equals(action)) {
                    Scheduler scheduler = Arc.container().instance(Scheduler.class).get();
                    if (!scheduler.isRunning()) {
                        scheduler.resume();
                        LOG.info("Scheduler resumed via Dev UI");
                        flashMessage(ctx, "Scheduler resumed");
                    }
                } else if ("pauseJob".equals(action)) {
                    Scheduler scheduler = Arc.container().instance(Scheduler.class).get();
                    String identity = form.get("identity");
                    if (identity != null && !scheduler.isPaused(identity)) {
                        scheduler.pause(identity);
                        LOG.infof("Scheduler paused job with identity '%s' via Dev UI", identity);
                        flashMessage(ctx, "Job with identity " + identity + " paused");
                    }
                } else if ("resumeJob".equals(action)) {
                    Scheduler scheduler = Arc.container().instance(Scheduler.class).get();
                    String identity = form.get("identity");
                    if (identity != null && scheduler.isPaused(identity)) {
                        scheduler.resume(identity);
                        LOG.infof("Scheduler resumed job with identity '%s'via Dev UI", identity);
                        flashMessage(ctx, "Job with identity " + identity + " resumed");
                    }
                } else {
                    String name = form.get("name");
                    SchedulerContext context = Arc.container().instance(SchedulerContext.class).get();
                    for (ScheduledMethod metadata : context.getScheduledMethods()) {
                        if (metadata.getMethodDescription().equals(name)) {
                            Vertx vertx = Arc.container().instance(Vertx.class).get();
                            Context vdc = VertxContext.getOrCreateDuplicatedContext(vertx);
                            VertxContextSafetyToggle.setContextSafe(vdc, true);
                            try {
                                ScheduledInvoker invoker = context
                                        .createInvoker(metadata.getInvokerClassName());
                                if (invoker.isBlocking()) {
                                    vdc.executeBlocking(p -> {
                                        try {
                                            invoker.invoke(new DevModeScheduledExecution());
                                        } catch (Exception ignored) {
                                        } finally {
                                            p.complete();
                                        }
                                    }, false);
                                } else {
                                    vdc.runOnContext(x -> {
                                        try {
                                            invoker.invoke(new DevModeScheduledExecution());
                                        } catch (Exception ignored) {
                                        }
                                    });
                                }
                                LOG.infof("Invoked scheduled method %s via Dev UI", name);
                            } catch (Exception e) {
                                LOG.error(
                                        "Unable to invoke a @Scheduled method: "
                                                + metadata.getMethodDescription(),
                                        e);
                            }
                            flashMessage(ctx, "Action invoked");
                            return;
                        }
                    }
                    flashMessage(ctx, "Action not found: " + name, FlashMessageStatus.ERROR);
                }
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

                @Override
                public boolean isOverdue() {
                    return false;
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
