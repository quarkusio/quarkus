package io.quarkus.builder;

import static io.quarkus.builder.Execution.log;

import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
     * Consume all the values produced for the named item.
     *
     * @param type the item element type (must not be {@code null})
     * @return the produced items as an immutable list (may be empty, will not be {@code null})
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
        return Collections.unmodifiableList(execution.getMultis().get(id));
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
    @Deprecated(since = "3.32", forRemoval = true)
    public <T extends MultiBuildItem> List<T> consumeMulti(Class<T> type, Comparator<? super T> comparator) {
        // you need to make a copy before sorting as the result of consumeMulti(type) is immutable
        return consumeMulti(type).stream().sorted(comparator).toList();
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

    /**
     * Get the build step dependency graph as an adjacency map.
     * Each key is a build step ID; the corresponding value is the set
     * of step IDs that the key step depends on (its dependencies).
     * <p>
     * This traverses the full step graph by inverting the forward
     * (dependent) edges stored in each {@link StepInfo}. The result
     * is computed on each call and not cached.
     *
     * @return the dependency graph (step ID → set of dependency step IDs)
     */
    /**
     * Get the ID of the currently executing build step.
     *
     * @return the step ID (never {@code null})
     */
    public String getStepId() {
        return stepInfo.getBuildStep().getId();
    }

    /**
     * Build item types whose production/consumption edges should be excluded
     * from the step dependency graph. These items are consumed only by
     * {@code MainClassBuildStep} for code generation and do not represent
     * runtime ordering dependencies between services.
     */
    private static final Set<String> EXCLUDED_EDGE_ITEMS = Set.of(
            "io.quarkus.deployment.builditem.StaticBytecodeRecorderBuildItem",
            "io.quarkus.deployment.builditem.MainBytecodeRecorderBuildItem");

    public Map<String, Set<String>> getStepDependencyGraph() {
        Map<String, Set<String>> result = new HashMap<>();
        Set<StepInfo> visited = new HashSet<>();
        Deque<StepInfo> work = new ArrayDeque<>(execution.getBuildChain().getStartSteps());
        while (!work.isEmpty()) {
            StepInfo step = work.poll();
            if (!visited.add(step)) {
                continue;
            }
            String stepId = step.getBuildStep().getId();
            result.computeIfAbsent(stepId, k -> new HashSet<>());
            for (StepInfo dependent : step.getDependents()) {
                // skip edges where the only connecting items are recorder build items
                if (isRecorderOnlyEdge(step, dependent)) {
                    work.add(dependent);
                    continue;
                }
                String dependentId = dependent.getBuildStep().getId();
                // dependent depends on step → add step as a dependency of dependent
                result.computeIfAbsent(dependentId, k -> new HashSet<>()).add(stepId);
                work.add(dependent);
            }
        }
        return result;
    }

    /**
     * Check if the dependency edge between a producer and consumer step
     * exists solely because of recorder build item types. If the only
     * items that the consumer consumes AND the producer produces are
     * excluded item types, the edge is spurious.
     */
    private static boolean isRecorderOnlyEdge(StepInfo producer, StepInfo consumer) {
        Set<ItemId> produced = producer.getProduces();
        Set<ItemId> consumed = consumer.getConsumes();
        boolean hasRealOverlap = false;
        boolean hasExcludedOverlap = false;
        for (ItemId p : produced) {
            if (consumed.contains(p)) {
                if (EXCLUDED_EDGE_ITEMS.contains(p.getType().getName())) {
                    hasExcludedOverlap = true;
                } else {
                    hasRealOverlap = true;
                }
            }
        }
        return hasExcludedOverlap && !hasRealOverlap;
    }

    /**
     * Get a mapping from build item class name to the set of step IDs that
     * produce that build item. Used by the service graph builder to resolve
     * {@code afterBuildItem()} declarations to producing steps.
     *
     * @return a map of build item class name → producing step IDs
     */
    public Map<String, Set<String>> getBuildItemProducers() {
        Map<String, Set<String>> result = new HashMap<>();
        Set<StepInfo> visited = new HashSet<>();
        Deque<StepInfo> work = new ArrayDeque<>(execution.getBuildChain().getStartSteps());
        while (!work.isEmpty()) {
            StepInfo step = work.poll();
            if (!visited.add(step)) {
                continue;
            }
            String stepId = step.getBuildStep().getId();
            for (ItemId produced : step.getAllProduces()) {
                result.computeIfAbsent(produced.getType().getName(), k -> new HashSet<>()).add(stepId);
            }
            for (StepInfo dependent : step.getDependents()) {
                work.add(dependent);
            }
        }
        return result;
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
            execution.getMultis().put(stepInfo.getOrdinal(), id, (MultiBuildItem) value);
        } else {
            if (execution.getSingles().putIfAbsent(id, value) != null) {
                throw Messages.msg.cannotMulti(id);
            }
        }
        execution.getMetrics().buildItemProduced(stepInfo, value);
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
