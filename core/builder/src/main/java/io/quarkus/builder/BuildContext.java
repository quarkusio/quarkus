package io.quarkus.builder;

import static io.quarkus.builder.Execution.log;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.quarkus.builder.diag.Diagnostic;
import io.quarkus.builder.item.BuildItem;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.builder.location.Location;
import io.smallrye.common.constraint.Assert;

/**
 * The context passed to a deployer's operation.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class BuildContext {
    private final ClassLoader classLoader;
    private final StepInfo stepInfo;
    private final Execution execution;
    private final AtomicInteger dependencies;
    private volatile boolean running;

    BuildContext(ClassLoader classLoader, final StepInfo stepInfo, final Execution execution) {
        this.classLoader = classLoader;
        this.stepInfo = stepInfo;
        this.execution = execution;
        dependencies = new AtomicInteger(stepInfo.getDependencies());
    }

    /**
     * Get the name of this build target. The resultant string is useful for diagnostic messages and does not have
     * any other significance.
     *
     * @return the name of this build target (not {@code null})
     */
    public String getBuildTargetName() {
        return execution.getBuildTargetName();
    }

    /**
     * Produce the given item. If the {@code type} refers to an item which is declared with multiplicity, then this
     * method can be called more than once for the given {@code type}, otherwise it must be called no more than once.
     *
     * @param item the item value (must not be {@code null})
     * @throws IllegalArgumentException if the item does not allow multiplicity but this method is called more than one time,
     *         or if the type of item could not be determined
     */
    public void produce(BuildItem item) {
        Assert.checkNotNullParam("item", item);
        doProduce(new ItemId(item.getClass()), item);
    }

    /**
     * Produce the given items. This method can be called more than once for the given {@code type}
     *
     * @param items the items (must not be {@code null})
     * @throws IllegalArgumentException if the type of item could not be determined
     */
    public void produce(List<? extends MultiBuildItem> items) {
        Assert.checkNotNullParam("items", items);
        for (MultiBuildItem item : items) {
            doProduce(new ItemId(item.getClass()), item);
        }
    }

    /**
     * Produce the given item. If the {@code type} refers to an item which is declared with multiplicity, then this
     * method can be called more than once for the given {@code type}, otherwise it must be called no more than once.
     *
     * @param type the item type (must not be {@code null})
     * @param item the item value (may be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to produce {@code type}, or if {@code type} is
     *         {@code null}, or if
     *         the item does not allow multiplicity but this method is called more than one time
     */
    public <T extends BuildItem> void produce(Class<T> type, T item) {
        Assert.checkNotNullParam("type", type);
        doProduce(new ItemId(type), type.cast(item));
    }

    /**
     * Consume the value produced for the named item.
     *
     * @param type the item type (must not be {@code null})
     * @return the produced item (may be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to consume {@code type}, or if {@code type} is
     *         {@code null}
     * @throws ClassCastException if the cast failed
     */
    public <T extends SimpleBuildItem> T consume(Class<T> type) {
        Assert.checkNotNullParam("type", type);
        if (!running) {
            throw Messages.msg.buildStepNotRunning();
        }
        final ItemId id = new ItemId(type);
        if (id.isMulti()) {
            throw Messages.msg.cannotMulti(id);
        }
        if (!stepInfo.getConsumes().contains(id)) {
            throw Messages.msg.undeclaredItem(id);
        }
        return type.cast(execution.getSingles().get(id));
    }

    /**
     * Consume all the values produced for the named item. If the
     * item type implements {@link Comparable}, it will be sorted by natural order before return. The returned list
     * is a mutable copy.
     *
     * @param type the item element type (must not be {@code null})
     * @return the produced items (may be empty, will not be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to consume {@code type}, or if {@code type} is
     *         {@code null}
     */
    public <T extends MultiBuildItem> List<T> consumeMulti(Class<T> type) {
        Assert.checkNotNullParam("type", type);
        if (!running) {
            throw Messages.msg.buildStepNotRunning();
        }
        final ItemId id = new ItemId(type);
        if (!id.isMulti()) {
            // can happen if obj changes base class
            throw Messages.msg.cannotMulti(id);
        }
        if (!stepInfo.getConsumes().contains(id)) {
            throw Messages.msg.undeclaredItem(id);
        }
        return new ArrayList<>((List<T>) (List) execution.getMultis().getOrDefault(id, Collections.emptyList()));
    }

    /**
     * Consume all the values produced for the named item, re-sorting it according
     * to the given comparator. The returned list is a mutable copy.
     *
     * @param type the item element type (must not be {@code null})
     * @param comparator the comparator to use (must not be {@code null})
     * @return the produced items (may be empty, will not be {@code null})
     * @throws IllegalArgumentException if this deployer was not declared to consume {@code type}, or if {@code type} is
     *         {@code null}
     */
    public <T extends MultiBuildItem> List<T> consumeMulti(Class<T> type, Comparator<? super T> comparator) {
        final List<T> result = consumeMulti(type);
        result.sort(comparator);
        return result;
    }

    /**
     * Emit a build note. This indicates information that the user may be interested in.
     *
     * @param location the location of interest (may be {@code null})
     * @param format the format string (see {@link String#format(String, Object...)})
     * @param args the format arguments
     */
    public void note(Location location, String format, Object... args) {
        final List<Diagnostic> list = execution.getDiagnostics();
        list.add(new Diagnostic(Diagnostic.Level.NOTE, location, format, args));
    }

    /**
     * Emit a build warning. This indicates a significant build problem that the user should be made aware of.
     *
     * @param location the location of interest (may be {@code null})
     * @param format the format string (see {@link String#format(String, Object...)})
     * @param args the format arguments
     */
    public void warn(Location location, String format, Object... args) {
        final List<Diagnostic> list = execution.getDiagnostics();
        list.add(new Diagnostic(Diagnostic.Level.WARN, location, format, args));
    }

    /**
     * Emit a build error. This indicates a build problem that prevents the build from proceeding.
     *
     * @param location the location of interest (may be {@code null})
     * @param format the format string (see {@link String#format(String, Object...)})
     * @param args the format arguments
     */
    public void error(Location location, String format, Object... args) {
        final List<Diagnostic> list = execution.getDiagnostics();
        list.add(new Diagnostic(Diagnostic.Level.ERROR, location, format, args));
        execution.setErrorReported();
    }

    /**
     * Get an executor which can be used for asynchronous tasks.
     *
     * @return an executor which can be used for asynchronous tasks
     */
    public ExecutorService getExecutor() {
        return execution.getExecutor();
    }

    // -- //

    private void doProduce(ItemId id, BuildItem value) {
        if (!running) {
            throw Messages.msg.buildStepNotRunning();
        }
        if (!stepInfo.getProduces().contains(id)) {
            throw Messages.msg.undeclaredItem(id);
        }
        if (id.isMulti()) {
            final List<BuildItem> list = execution.getMultis().computeIfAbsent(id, x -> new ArrayList<>());
            synchronized (list) {
                if (Comparable.class.isAssignableFrom(id.getType())) {
                    int pos = Collections.binarySearch((List) list, value);
                    if (pos < 0)
                        pos = -(pos + 1);
                    list.add(pos, value);
                } else {
                    list.add(value);
                }
            }
        } else {
            if (execution.getSingles().putIfAbsent(id, value) != null) {
                throw Messages.msg.cannotMulti(id);
            }
        }
        execution.getMetrics().buildItemProduced(value);
    }

    void depFinished() {
        final int remaining = dependencies.decrementAndGet();
        log.tracef("Dependency of \"%2$s\" finished; %1$d remaining", remaining, stepInfo.getBuildStep());
        if (remaining == 0) {
            execution.getExecutor().execute(this::run);
        }
    }

    void run() {
        final Execution execution = this.execution;
        final StepInfo stepInfo = this.stepInfo;
        final BuildStep buildStep = stepInfo.getBuildStep();
        final long start = System.nanoTime();
        final LocalTime started = LocalTime.now();
        final Thread currentThread = Thread.currentThread();
        log.tracef("Starting step \"%s\"", buildStep);
        try {
            if (!execution.isErrorReported()) {
                running = true;
                ClassLoader old = currentThread.getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(classLoader);
                    buildStep.execute(this);
                } catch (Throwable t) {
                    final List<Diagnostic> list = execution.getDiagnostics();
                    list.add(new Diagnostic(Diagnostic.Level.ERROR, t, null, "Build step %s threw an exception", buildStep));
                    execution.setErrorReported();
                } finally {
                    running = false;
                    currentThread.setContextClassLoader(old);
                }
            }
        } finally {
            long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            execution.getMetrics().buildStepFinished(stepInfo, currentThread.getName(), started, duration);
            log.tracef("Finished step \"%s\" in %s ms", buildStep, duration);
            execution.removeBuildContext(stepInfo, this);
        }
        final Set<StepInfo> dependents = stepInfo.getDependents();
        if (!dependents.isEmpty()) {
            for (StepInfo info : dependents) {
                execution.getBuildContext(info).depFinished();
            }
        } else {
            execution.depFinished();
        }
    }
}
