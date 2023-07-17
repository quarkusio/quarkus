package io.quarkus.deployment.builditem;

import org.jboss.logging.Logger;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build Item that can be used to queue shutdown tasks that are run when the runtime application shuts down.
 *
 * This is similar to {@link ShutdownContextBuildItem} however it applies to tasks on the 'build' side, so if a processor
 * wants to close something after the application has completed this item lets it do this.
 *
 * This has no effect for production applications, and is only useful in dev/test mode. The main use case for this is
 * for shutting down deployment side test utilities at the end of a test run.
 */
public final class RuntimeApplicationShutdownBuildItem extends MultiBuildItem {

    private static final Logger log = Logger.getLogger(RuntimeApplicationShutdownBuildItem.class);

    private final Runnable closeTask;

    public RuntimeApplicationShutdownBuildItem(Runnable closeTask) {
        this.closeTask = closeTask;
    }

    public Runnable getCloseTask() {
        return closeTask;
    }
}
