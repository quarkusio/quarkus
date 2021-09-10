package io.quarkus.deployment.builditem;

import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Build Item that can be used to queue shutdown tasks that are run when the {@link io.quarkus.bootstrap.app.CuratedApplication}
 * is closed.
 * <p>
 * For production applications this will be at the end of the maven/gradle build, for dev mode applications this will be
 * when dev mode shuts down, for tests it will generally be at the end of the test run, however for continuous testing this
 * will be when the outer dev mode process shuts down. For unit style tests this will generally be the end of the test.
 */
public final class CuratedApplicationShutdownBuildItem extends SimpleBuildItem {

    private static final Logger log = Logger.getLogger(CuratedApplicationShutdownBuildItem.class);

    private final boolean firstRun;
    private final CopyOnWriteArrayList<Runnable> tasks = new CopyOnWriteArrayList<>();
    private final QuarkusClassLoader baseCl;
    boolean registered;

    public CuratedApplicationShutdownBuildItem(QuarkusClassLoader baseCl, boolean firstRun) {
        this.firstRun = firstRun;
        this.baseCl = baseCl;
    }

    /**
     * Adds a task to run when the application is closed
     *
     * @param task The task
     * @param firstRunOnly If this should only be added for the first augment step. This makes it possible to prevent tasks
     *        being added for every build
     */
    public synchronized void addCloseTask(Runnable task, boolean firstRunOnly) {
        if (firstRunOnly || firstRun) {
            if (!registered) {
                registered = true;
                baseCl.addCloseTask(new Runnable() {
                    @Override
                    public void run() {
                        for (var i : tasks) {
                            try {
                                i.run();
                            } catch (Throwable t) {
                                log.error("Failed to run close task", t);
                            }
                        }
                    }
                });
            }
            tasks.add(task);
        }
    }
}
