package io.quarkus.deployment.steps;

import io.quarkus.core.deployment.action.ActionBuilder;
import io.quarkus.core.impl.LastShutdownTasks;
import io.quarkus.deployment.Phase;
import io.quarkus.deployment.annotations.BuildStep;

/**
 * Registers a static-init service that resets the thread context classloader
 * (TCCL) on any thread that still holds a QuarkusClassLoader after shutdown.
 * <p>
 * Infrastructure threads (JFR, testcontainers-ryuk, Groovy PIC-Cleaner, etc.)
 * may be created during startup and inherit the QuarkusClassLoader as their
 * TCCL. If these threads outlive the application (common in test mode with
 * multiple restarts), they pin the classloader, preventing class unloading
 * and causing metaspace OOME.
 * <p>
 * This cleanup runs as part of the static-init graph's stop cascade (after
 * the runtime-init graph has fully stopped) to ensure all application threads
 * have finished before we reset TCCLs.
 */
public class ThreadClassLoaderCleanupStep {

    private static final String QCL_CLASS_NAME = "io.quarkus.bootstrap.classloading.QuarkusClassLoader";

    @BuildStep
    void registerTcclCleanup(ActionBuilder action) {
        action
                .forService("io.quarkus.core.tccl-cleanup")
                .atPhase(Phase.STATIC_INIT)
                .before("io.quarkus.core.last-shutdown-tasks")
                .action(ctx -> {
                    // capture the specific QCL instance being shut down —
                    // only reset threads that hold THIS classloader, not
                    // a newer QCL from the next test restart.
                    // In production/native mode the TCCL is not a QCL; skip cleanup entirely.
                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    ClassLoader currentQcl = (cl != null && cl.getClass().getName().equals(QCL_CLASS_NAME)) ? cl : null;
                    ctx.onStop(() -> {
                        if (currentQcl == null) {
                            return;
                        }
                        ClassLoader systemCl = ClassLoader.getSystemClassLoader();
                        for (Thread t : Thread.getAllStackTraces().keySet()) {
                            if (t == Thread.currentThread()) {
                                continue;
                            }
                            try {
                                ClassLoader tcl = t.getContextClassLoader();
                                if (tcl != null && tcl == currentQcl) {
                                    t.setContextClassLoader(systemCl);
                                }
                            } catch (SecurityException ignored) {
                            }
                        }
                    });
                });
    }

    /**
     * Runs global "last" shutdown tasks collected via
     * {@link io.quarkus.runtime.ShutdownContext#addLastShutdownTask(Runnable)}.
     * <p>
     * The ArcContainer service declares {@code after("io.quarkus.core.last-shutdown-tasks")}
     * so it stops before this service — bean destruction (and OTel flush etc.)
     * complete before "last" tasks (like SmallRye Context Propagation cleanup) run.
     */
    @BuildStep
    void registerLastShutdownTasks(ActionBuilder action) {
        action
                .forService("io.quarkus.core.last-shutdown-tasks")
                .atPhase(Phase.STATIC_INIT)
                .action(ctx -> {
                    ctx.onStop(LastShutdownTasks::run);
                });
    }
}
