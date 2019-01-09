/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.builder;

import static java.lang.Math.max;
import static java.util.concurrent.locks.LockSupport.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.builder.diag.Diagnostic;
import org.jboss.builder.item.BuildItem;
import org.jboss.logging.Logger;
import org.jboss.threads.EnhancedQueueExecutor;
import org.jboss.threads.JBossExecutors;
import org.jboss.threads.JBossThreadFactory;

/**
 */
final class Execution {

    static final Logger log = Logger.getLogger("org.jboss.builder");

    private final BuildChain chain;
    private final ConcurrentHashMap<ItemId, BuildItem> singles;
    private final ConcurrentHashMap<ItemId, List<BuildItem>> multis;
    private final Set<ItemId> finalIds;
    private final ConcurrentHashMap<StepInfo, BuildContext> contextCache = new ConcurrentHashMap<>();
    private final EnhancedQueueExecutor executor;
    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private final String buildTargetName;
    private final AtomicBoolean errorReported = new AtomicBoolean();
    private final AtomicInteger lastStepCount = new AtomicInteger();
    private volatile Thread runningThread;
    private volatile boolean done;

    Execution(final BuildExecutionBuilder builder, final Set<ItemId> finalIds) {
        chain = builder.getChain();
        this.singles = new ConcurrentHashMap<>(builder.getInitialSingle());
        this.multis = new ConcurrentHashMap<>(builder.getInitialMulti());
        this.finalIds = finalIds;
        final EnhancedQueueExecutor.Builder executorBuilder = new EnhancedQueueExecutor.Builder();
        executorBuilder.setCorePoolSize(8).setMaximumPoolSize(1024);
        executorBuilder.setExceptionHandler(JBossExecutors.loggingExceptionHandler());
        executorBuilder.setThreadFactory(new JBossThreadFactory(new ThreadGroup("build group"), Boolean.FALSE, null, "build-%t", JBossExecutors.loggingExceptionHandler(), null));
        buildTargetName = builder.getBuildTargetName();
        executor = executorBuilder.build();
        lastStepCount.set(builder.getChain().getEndStepCount());
        if(lastStepCount.get() == 0)
            done = true;
    }

    List<Diagnostic> getDiagnostics() {
        return diagnostics;
    }

    BuildContext getBuildContext(StepInfo stepInfo) {
        return contextCache.computeIfAbsent(stepInfo, si -> new BuildContext(si, this));
    }

    void removeBuildContext(StepInfo stepInfo, BuildContext buildContext) {
        contextCache.remove(stepInfo, buildContext);
    }

    BuildResult run() throws BuildException {
        final long start = System.nanoTime();
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
                if (Thread.interrupted()) intr = true;
                if (done) break;
                park(this);
            }
        } finally {
            if (intr) Thread.currentThread().interrupt();
            runningThread = null;
        }
        executor.shutdown();
        for (;;) try {
            executor.awaitTermination(1000L, TimeUnit.DAYS);
            break;
        } catch (InterruptedException e) {
            intr = true;
        } finally {
            if (intr) Thread.currentThread().interrupt();
        }
        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic.getLevel() == Diagnostic.Level.ERROR) {
                BuildException failed = new BuildException("Build failed due to errors",diagnostic.getThrown(),  Collections.unmodifiableList(diagnostics));
                for(Diagnostic i : diagnostics) {
                    if(i.getThrown() != null && i.getThrown() != diagnostic.getThrown()) {
                        failed.addSuppressed(i.getThrown());
                    }
                }
                throw failed;
            }
        }
        if (lastStepCount.get() > 0) throw new BuildException("Extra steps left over", Collections.emptyList());
        return new BuildResult(singles, multis, finalIds, Collections.unmodifiableList(diagnostics), max(0, System.nanoTime() - start));
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

    void depFinished() {
        final int count = lastStepCount.decrementAndGet();
        log.tracef("End step completed; %d remaining", count);
        if (count == 0) {
            done = true;
            unpark(runningThread);
        }
    }
}
