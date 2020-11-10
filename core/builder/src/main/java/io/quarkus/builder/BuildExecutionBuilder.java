package io.quarkus.builder;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jboss.threads.EnhancedQueueExecutor;
import org.jboss.threads.JBossExecutors;
import org.jboss.threads.JBossThreadFactory;
import org.wildfly.common.Assert;

import io.quarkus.builder.item.BuildItem;
import io.quarkus.builder.item.EmptyBuildItem;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.qlue.ExecutionBuilder;
import io.quarkus.qlue.Result;

/**
 * A builder for a deployer execution.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class BuildExecutionBuilder {
    private final ExecutionBuilder executionBuilder;
    private final String buildTargetName;

    BuildExecutionBuilder(final ExecutionBuilder executionBuilder, final String buildTargetName) {
        this.executionBuilder = executionBuilder;
        this.buildTargetName = buildTargetName;
    }

    /**
     * Get the name of this build target. The resultant string is useful for diagnostic messages and does not have
     * any other significance.
     *
     * @return the name of this build target (not {@code null})
     */
    public String getBuildTargetName() {
        return buildTargetName;
    }

    /**
     * Provide an initial item.
     *
     * @param item the item value
     * @return this builder
     * @throws IllegalArgumentException if this deployer chain was not declared to initially produce {@code type},
     *         or if the item does not allow multiplicity but this method is called more than one time
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T extends BuildItem> BuildExecutionBuilder produce(T item) {
        Assert.checkNotNullParam("item", item);
        produce((Class) item.getClass(), item);
        return this;
    }

    /**
     * Provide an initial item.
     *
     * @param type the item type (must not be {@code null})
     * @param item the item value
     * @return this builder
     * @throws IllegalArgumentException if this deployer chain was not declared to initially produce {@code type},
     *         or if {@code type} is {@code null}, or if the item does not allow multiplicity but this method is called
     *         more than one time
     */
    public <T extends BuildItem> BuildExecutionBuilder produce(Class<T> type, T item) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("item", item);
        if (MultiBuildItem.class.isAssignableFrom(type)) {
            executionBuilder.produce(LegacyMultiItem.class, type.asSubclass(MultiBuildItem.class),
                    new LegacyMultiItem((MultiBuildItem) item));
        } else if (SimpleBuildItem.class.isAssignableFrom(type)) {
            executionBuilder.produce(LegacySimpleItem.class, type.asSubclass(SimpleBuildItem.class),
                    new LegacySimpleItem((SimpleBuildItem) item));
        } else {
            assert EmptyBuildItem.class.isAssignableFrom(type);
        }
        return this;
    }

    /**
     * Run the build. The chain may run in one or many threads.
     *
     * @return the build result (not {@code null})
     * @throws BuildException if build failed
     */
    public BuildResult execute() throws BuildException {
        final EnhancedQueueExecutor.Builder executorBuilder = new EnhancedQueueExecutor.Builder();
        executorBuilder.setRegisterMBean(false);
        executorBuilder.setCorePoolSize(8).setMaximumPoolSize(1024);
        executorBuilder.setExceptionHandler(JBossExecutors.loggingExceptionHandler());
        executorBuilder.setThreadFactory(new JBossThreadFactory(new ThreadGroup("build group"), Boolean.FALSE, null, "build-%t",
                JBossExecutors.loggingExceptionHandler(), null));
        EnhancedQueueExecutor executor = executorBuilder.build();
        try {
            Result result = executionBuilder.execute(executor);
            executor.shutdown(true);
            boolean intr = false;
            try {
                while (!executor.isTerminated())
                    try {
                        executor.awaitTermination(1L, TimeUnit.DAYS);
                    } catch (InterruptedException e) {
                        intr = true;
                    }
            } finally {
                if (intr) {
                    Thread.currentThread().interrupt();
                }
            }
            if (result.isFailure()) {
                List<Throwable> problems = result.asFailure().getProblems();
                Iterator<Throwable> iterator = problems.iterator();
                BuildException buildException;
                if (iterator.hasNext()) {
                    buildException = new BuildException("Build failed", iterator.next());
                    while (iterator.hasNext()) {
                        buildException.addSuppressed(iterator.next());
                    }
                } else {
                    buildException = new BuildException("Build failed (no cause recorded)");
                }
                throw buildException;
            } else {
                return new BuildResult(result.asSuccess());
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
