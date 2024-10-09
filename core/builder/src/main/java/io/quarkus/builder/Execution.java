package io.quarkus.builder;

import static java.lang.Math.max;
import static java.util.concurrent.locks.LockSupport.park;
import static java.util.concurrent.locks.LockSupport.unpark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.logging.Logger;
import org.jboss.threads.EnhancedQueueExecutor;
import org.jboss.threads.JBossExecutors;
import org.jboss.threads.JBossThreadFactory;

import io.quarkus.builder.diag.Diagnostic;
import io.quarkus.builder.item.BuildItem;

/**
 */
final class Execution {

    static final Logger log = Logger.getLogger("io.quarkus.builder");

    private final BuildChain chain;
    private final ConcurrentHashMap<ItemId, BuildItem> singles;
    private final ConcurrentHashMap<ItemId, List<BuildItem>> multis;
    private final Set<ItemId> finalIds;
    private final ConcurrentHashMap<StepInfo, BuildContext> contextCache = new ConcurrentHashMap<>();
    private final EnhancedQueueExecutor executor;
    private final List<Diagnostic> diagnostics = Collections.synchronizedList(new ArrayList<>());
    private final String buildTargetName;
    private final AtomicBoolean errorReported = new AtomicBoolean();
    private final AtomicInteger lastStepCount = new AtomicInteger();
    private volatile Thread runningThread;
    private volatile boolean done;

    private final BuildMetrics metrics;

    static {
        try {
            Class.forName("org.jboss.threads.EnhancedQueueExecutor$1", false, Execution.class.getClassLoader());
        } catch (ClassNotFoundException ignored) {
        }
    }

    Execution(final BuildExecutionBuilder builder, final Set<ItemId> finalIds) {
        chain = builder.getChain();
        this.singles = new ConcurrentHashMap<>(builder.getInitialSingle());
        this.multis = new ConcurrentHashMap<>(builder.getInitialMulti());
        this.finalIds = finalIds;
        final EnhancedQueueExecutor.Builder executorBuilder = new EnhancedQueueExecutor.Builder();
        executorBuilder.setRegisterMBean(false);
        executorBuilder.setQueueLimited(false);
        executorBuilder.setCorePoolSize(8).setMaximumPoolSize(1024);
        executorBuilder.setExceptionHandler(JBossExecutors.loggingExceptionHandler());
        executorBuilder.setThreadFactory(new JBossThreadFactory(new ThreadGroup("build group"), Boolean.FALSE, null, "build-%t",
                JBossExecutors.loggingExceptionHandler(), null));
        buildTargetName = builder.getBuildTargetName();
        executor = executorBuilder.build();
        lastStepCount.set(builder.getChain().getEndStepCount());
        if (lastStepCount.get() == 0)
            done = true;

        metrics = new BuildMetrics(buildTargetName);
    }

    List<Diagnostic> getDiagnostics() {
        return diagnostics;
    }

    BuildContext getBuildContext(StepInfo stepInfo) {
        return contextCache.computeIfAbsent(stepInfo, si -> new BuildContext(chain.getClassLoader(), si, this));
    }

    void removeBuildContext(StepInfo stepInfo, BuildContext buildContext) {
        contextCache.remove(stepInfo, buildContext);
    }

    BuildResult run() throws BuildException {
        final long start = System.nanoTime();
        metrics.buildStarted();
        runningThread = Thread.currentThread();

        // run the build
        final List<StepInfo> startSteps = chain.getStartSteps();
        for (StepInfo startStep : startSteps) {
            executor.execute(getBuildContext(startStep)::run);
        }
        // wait for the wrap-up
        boolean intr = false;
        try {
            for (;;) {
                if (Thread.interrupted())
                    intr = true;
                if (done)
                    break;
                park(this);
            }
        } finally {
            if (intr)
                Thread.currentThread().interrupt();
            runningThread = null;
        }
        executor.shutdown();
        for (;;)
            try {
                executor.awaitTermination(1000L, TimeUnit.DAYS);
                break;
            } catch (InterruptedException e) {
                intr = true;
            } finally {
                if (intr)
                    Thread.currentThread().interrupt();
            }
        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic.getLevel() == Diagnostic.Level.ERROR) {
                BuildException failed = new BuildException("Build failed due to errors", diagnostic.getThrown(),
                        Collections.unmodifiableList(diagnostics));
                for (Diagnostic i : diagnostics) {
                    if (i.getThrown() != null && i.getThrown() != diagnostic.getThrown()) {
                        failed.addSuppressed(i.getThrown());
                    }
                }
                throw failed;
            }
        }
        if (lastStepCount.get() > 0)
            throw new BuildException("Extra steps left over", Collections.emptyList());

        long duration = max(0, System.nanoTime() - start);
        metrics.buildFinished(TimeUnit.NANOSECONDS.toMillis(duration));
        return new BuildResult(singles, multis, finalIds, Collections.unmodifiableList(diagnostics),
                duration, metrics);
    }

    EnhancedQueueExecutor getExecutor() {
        return executor;
    }

    String getBuildTargetName() {
        return buildTargetName;
    }

    void setErrorReported() {
        errorReported.compareAndSet(false, true);
    }

    boolean isErrorReported() {
        return errorReported.get();
    }

    ConcurrentHashMap<ItemId, BuildItem> getSingles() {
        return singles;
    }

    ConcurrentHashMap<ItemId, List<BuildItem>> getMultis() {
        return multis;
    }

    BuildChain getBuildChain() {
        return chain;
    }

    BuildMetrics getMetrics() {
        return metrics;
    }

    void depFinished() {
        final int count = lastStepCount.decrementAndGet();
        log.tracef("End step completed; %d remaining", count);
        if (count == 0) {
            done = true;
            unpark(runningThread);
        }
    }
}
