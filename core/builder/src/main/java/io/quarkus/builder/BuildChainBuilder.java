package io.quarkus.builder;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.wildfly.common.Assert;

import io.quarkus.builder.item.BuildItem;

/**
 * A build chain builder.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class BuildChainBuilder {

    private static final String GRAPH_OUTPUT = System.getProperty("jboss.builder.graph-output");

    private final BuildStepBuilder finalStep;
    private final List<BuildProvider> providers = new ArrayList<>();
    private final Map<BuildStepBuilder, StackTraceElement[]> steps = new HashMap<BuildStepBuilder, StackTraceElement[]>();
    private final Set<ItemId> initialIds = new HashSet<>();
    private final Set<ItemId> finalIds = new HashSet<>();

    BuildChainBuilder() {
        finalStep = addBuildStep(new FinalStep());
    }

    /**
     * Add a build step to the chain. The configuration in the build step builder at the time that the chain is built is
     * the configuration that will apply to the build step in the final chain. Any subsequent changes will be ignored.
     * <p>
     * A given build step is included in the chain when one or more of the following criteria are met:
     * <ul>
     * <li>It includes a pre-produce step for a item which is produced by one or more build steps that is included in the
     * chain</li>
     * <li>It includes a produce step for a item which is consumed by a build step that is included in the chain or is a final
     * item</li>
     * <li>It includes a consume step for a item which is produced by a build step that is included in the chain or is an
     * initial item</li>
     * <li>It includes a destroy step for a item which is produced by a build step that is included in the chain or is an
     * initial item</li>
     * </ul>
     * In addition, the declaration of producers and consumers can cause corresponding consumers and producers to be
     * included if they exist.
     *
     * @param buildStep the build step instance
     * @return the builder for the build step
     */
    public BuildStepBuilder addBuildStep(BuildStep buildStep) {
        final BuildStepBuilder buildStepBuilder = new BuildStepBuilder(this);
        buildStepBuilder.setBuildStep(buildStep);
        return buildStepBuilder;
    }

    /**
     * Add a build step to the chain. The configuration in the build step builder at the time that the chain is built is
     * the configuration that will apply to the build step in the final chain. Any subsequent changes will be ignored.
     * <p>
     * A given build step is included in the chain when one or more of the following criteria are met:
     * <ul>
     * <li>It includes a pre-produce step for a item which is produced by one or more build steps that is included in the
     * chain</li>
     * <li>It includes a produce step for a item which is consumed by a build step that is included in the chain or is a final
     * item</li>
     * <li>It includes a consume step for a item which is produced by a build step that is included in the chain or is an
     * initial item</li>
     * <li>It includes a destroy step for a item which is produced by a build step that is included in the chain or is an
     * initial item</li>
     * </ul>
     * In addition, the declaration of producers and consumers can cause corresponding consumers and producers to be
     * included if they exist.
     *
     * @return the builder for the build step
     */
    public BuildStepBuilder addBuildStep() {
        return new BuildStepBuilder(this);
    }

    /**
     * Declare an initial item that will be provided to build steps in the chain. Note that if this method is called
     * for a simple item, no build steps will be allowed to produce that item.
     *
     * @param type the item type (must not be {@code null})
     * @return this builder
     * @throws IllegalArgumentException if the item type is {@code null}
     */
    public BuildChainBuilder addInitial(Class<? extends BuildItem> type) {
        Assert.checkNotNullParam("type", type);
        initialIds.add(new ItemId(type));
        return this;
    }

    public BuildChainBuilder loadProviders(ClassLoader classLoader) throws ChainBuildException {
        final ServiceLoader<BuildProvider> serviceLoader = ServiceLoader.load(BuildProvider.class, classLoader);
        for (final BuildProvider provider : serviceLoader) {
            provider.installInto(this);
        }
        return this;
    }

    /**
     * Declare a final item that will be consumable after the build step chain completes. This may be any item
     * that is produced in the chain.
     *
     * @param type the item type (must not be {@code null})
     * @return this builder
     * @throws IllegalArgumentException if the item type is {@code null}
     */
    public BuildChainBuilder addFinal(Class<? extends BuildItem> type) {
        Assert.checkNotNullParam("type", type);
        finalIds.add(new ItemId(type));
        return this;
    }

    /**
     * Build the build step chain from the current builder configuration.
     *
     * @return the constructed build chain
     * @throws ChainBuildException if the chain could not be built
     */
    public BuildChain build() throws ChainBuildException {
        final Set<ItemId> consumed = new HashSet<>();
        final Map<BuildStepBuilder, StepInfo> mappedSteps = new HashMap<>();
        int initialSingleCount = 0;
        int initialMultiCount = 0;
        final Map<BuildStepBuilder, StackTraceElement[]> steps = this.steps;
        // compile master produce/consume maps
        final Map<ItemId, List<Consume>> allConsumes = new HashMap<>();
        final Map<ItemId, List<Produce>> allProduces = new HashMap<>();
        final Set<ItemId> initialIds = this.initialIds;
        for (Map.Entry<BuildStepBuilder, StackTraceElement[]> stepEntry : steps.entrySet()) {
            final BuildStepBuilder stepBuilder = stepEntry.getKey();
            final Map<ItemId, Consume> stepConsumes = stepBuilder.getConsumes();
            for (Map.Entry<ItemId, Consume> entry : stepConsumes.entrySet()) {
                final ItemId id = entry.getKey();
                final List<Consume> list = allConsumes.computeIfAbsent(id, x -> new ArrayList<>(4));
                list.add(entry.getValue());
            }
            final Map<ItemId, Produce> stepProduces = stepBuilder.getProduces();
            for (Map.Entry<ItemId, Produce> entry : stepProduces.entrySet()) {
                final ItemId id = entry.getKey();
                final List<Produce> list = allProduces.computeIfAbsent(id, x -> new ArrayList<>(2));
                final Produce toBeAdded = entry.getValue();
                if (!id.isMulti() && toBeAdded.getConstraint() == Constraint.REAL) {
                    // ensure only one producer
                    if (initialIds.contains(id)) {
                        final ChainBuildException cbe = new ChainBuildException(
                                "Item " + id + " cannot be produced here (it is an initial resource) ("
                                        + toBeAdded.getStepBuilder().getBuildStep() + ")");
                        cbe.setStackTrace(steps.get(toBeAdded.getStepBuilder()));
                        throw cbe;
                    }
                    final boolean overridable = toBeAdded.isOverridable();
                    for (Produce produce : list) {
                        if (produce.getConstraint() == Constraint.REAL
                                && produce.isOverridable() == overridable) {
                            final Throwable cause = new Throwable("This is the location of the conflicting producer ("
                                    + toBeAdded.getStepBuilder().getBuildStep() + ")");
                            cause.setStackTrace(steps.get(toBeAdded.getStepBuilder()));
                            final ChainBuildException cbe = new ChainBuildException(
                                    String.format("Multiple %s" + "producers of item %s (%s)",
                                            overridable ? "overridable " : "", id, produce.getStepBuilder().getBuildStep()),
                                    cause);
                            cbe.setStackTrace(steps.get(produce.getStepBuilder()));
                            throw cbe;
                        }
                    }
                }
                list.add(toBeAdded);
            }
        }
        final Set<BuildStepBuilder> included = new HashSet<>();
        // now begin to wire dependencies
        final Set<ItemId> finalIds = this.finalIds;
        final ArrayDeque<BuildStepBuilder> toAdd = new ArrayDeque<>();
        final Set<Produce> lastDependencies = new HashSet<>();
        for (ItemId finalId : finalIds) {
            addOne(allProduces, included, toAdd, finalId, lastDependencies);
        }
        // now recursively add producers of consumed items
        BuildStepBuilder stepBuilder;
        Map<BuildStepBuilder, Set<Produce>> dependencies = new HashMap<>();
        while ((stepBuilder = toAdd.pollFirst()) != null) {
            for (Map.Entry<ItemId, Consume> entry : stepBuilder.getConsumes().entrySet()) {
                final Consume consume = entry.getValue();
                final ItemId id = entry.getKey();
                if (!consume.getFlags().contains(ConsumeFlag.OPTIONAL) && !id.isMulti()) {
                    if (!initialIds.contains(id) && !allProduces.containsKey(id)) {
                        throw new ChainBuildException("No producers for required item " + id);
                    }
                }
                // add every producer
                addOne(allProduces, included, toAdd, id, dependencies.computeIfAbsent(stepBuilder, x -> new HashSet<>()));
            }
        }
        // calculate dependents
        Map<BuildStepBuilder, Set<BuildStepBuilder>> dependents = new HashMap<>();
        for (Map.Entry<BuildStepBuilder, Set<Produce>> entry : dependencies.entrySet()) {
            final BuildStepBuilder dependent = entry.getKey();
            for (Produce produce : entry.getValue()) {
                dependents.computeIfAbsent(produce.getStepBuilder(), x -> new HashSet<>()).add(dependent);
            }
        }
        // detect cycles
        cycleCheck(included, new HashSet<>(), new HashSet<>(), dependencies, new ArrayDeque<>());
        // recursively build all
        final Set<StepInfo> startSteps = new HashSet<>();
        final Set<StepInfo> endSteps = new HashSet<>();
        for (BuildStepBuilder builder : included) {
            buildOne(builder, included, mappedSteps, dependents, dependencies, startSteps, endSteps);
        }
        if (GRAPH_OUTPUT != null && !GRAPH_OUTPUT.isEmpty()) {
            try (FileOutputStream fos = new FileOutputStream(GRAPH_OUTPUT)) {
                try (OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                    try (BufferedWriter writer = new BufferedWriter(osw)) {
                        writer.write("digraph {");
                        writer.newLine();
                        writer.write("    node [shape=rectangle];");
                        writer.newLine();
                        writer.write("    rankdir=LR;");
                        writer.newLine();
                        writer.newLine();
                        writer.write("    { rank = same; ");
                        for (StepInfo startStep : startSteps) {
                            writer.write(quoteString(startStep.getBuildStep().toString()));
                            writer.write("; ");
                        }
                        writer.write("};");
                        writer.newLine();
                        writer.write("    { rank = same; ");
                        for (StepInfo endStep : endSteps) {
                            if (!startSteps.contains(endStep)) {
                                writer.write(quoteString(endStep.getBuildStep().toString()));
                                writer.write("; ");
                            }
                        }
                        writer.write("};");
                        writer.newLine();
                        writer.newLine();
                        final HashSet<StepInfo> printed = new HashSet<>();
                        for (StepInfo step : startSteps) {
                            writeStep(writer, printed, step);
                        }
                        writer.write("}");
                        writer.newLine();
                    }
                }
            } catch (IOException ioe) {
                throw new RuntimeException("Failed to write debug graph output", ioe);
            }
        }
        return new BuildChain(initialSingleCount, initialMultiCount, startSteps, consumed, this, endSteps.size());
    }

    private static void writeStep(final BufferedWriter writer, final HashSet<StepInfo> printed, final StepInfo step)
            throws IOException {
        if (printed.add(step)) {
            final String currentStepName = quoteString(step.getBuildStep().toString());
            final Set<StepInfo> dependents = step.getDependents();
            if (!dependents.isEmpty()) {
                for (StepInfo dependent : dependents) {
                    final String dependentName = quoteString(dependent.getBuildStep().toString());
                    writer.write("    ");
                    writer.write(dependentName);
                    writer.write(" -> ");
                    writer.write(currentStepName);
                    writer.newLine();
                }
                writer.newLine();
                for (StepInfo dependent : dependents) {
                    writeStep(writer, printed, dependent);
                }
            }
        }
    }

    private static final Pattern QUOTE_PATTERN = Pattern.compile("[\"]");

    private static String quoteString(String input) {
        final Matcher matcher = QUOTE_PATTERN.matcher(input);
        final StringBuffer buf = new StringBuffer();
        buf.append('"');
        while (matcher.find()) {
            matcher.appendReplacement(buf, "\\" + matcher.group(0));
        }
        matcher.appendTail(buf);
        buf.append('"');
        return buf.toString();
    }

    private void cycleCheck(Set<BuildStepBuilder> builders, Set<BuildStepBuilder> visited, Set<BuildStepBuilder> checked,
            final Map<BuildStepBuilder, Set<Produce>> dependencies, final Deque<Produce> producedPath)
            throws ChainBuildException {
        for (BuildStepBuilder builder : builders) {
            cycleCheck(builder, visited, checked, dependencies, producedPath);
        }
    }

    private void cycleCheckProduce(Set<Produce> produceSet, Set<BuildStepBuilder> visited, Set<BuildStepBuilder> checked,
            final Map<BuildStepBuilder, Set<Produce>> dependencies, final Deque<Produce> producedPath)
            throws ChainBuildException {
        for (Produce produce : produceSet) {
            producedPath.add(produce);
            cycleCheck(produce.getStepBuilder(), visited, checked, dependencies, producedPath);
            producedPath.removeLast();
        }
    }

    private void cycleCheck(BuildStepBuilder builder, Set<BuildStepBuilder> visited, Set<BuildStepBuilder> checked,
            final Map<BuildStepBuilder, Set<Produce>> dependencies, final Deque<Produce> producedPath)
            throws ChainBuildException {
        if (!checked.contains(builder)) {
            if (!visited.add(builder)) {
                final StringBuilder b = new StringBuilder("Cycle detected:\n\t\t   ");
                final Iterator<Produce> itr = producedPath.descendingIterator();
                if (itr.hasNext()) {
                    Produce produce = itr.next();
                    for (;;) {
                        b.append(produce.getStepBuilder().getBuildStep());
                        ItemId itemId = produce.getItemId();
                        b.append(" produced ").append(itemId);
                        b.append("\n\t\tto ");
                        if (!itr.hasNext())
                            break;
                        produce = itr.next();
                        if (produce.getStepBuilder() == builder)
                            break;
                    }
                    b.append(builder.getBuildStep());
                }
                throw new ChainBuildException(b.toString());
            }
            try {
                final Set<Produce> dependencySet = dependencies.getOrDefault(builder, Collections.emptySet());
                cycleCheckProduce(dependencySet, visited, checked, dependencies, producedPath);
            } finally {
                visited.remove(builder);
            }
        }
        checked.add(builder);
    }

    private void addOne(final Map<ItemId, List<Produce>> allProduces, final Set<BuildStepBuilder> included,
            final ArrayDeque<BuildStepBuilder> toAdd, final ItemId idToAdd, Set<Produce> dependencies)
            throws ChainBuildException {
        boolean modified = false;
        for (Produce produce : allProduces.getOrDefault(idToAdd, Collections.emptyList())) {
            final BuildStepBuilder stepBuilder = produce.getStepBuilder();
            // if overridable, add in second pass only if this pass didn't add any producers
            if (!produce.getFlags().contains(ProduceFlag.OVERRIDABLE)) {
                if (!produce.getFlags().contains(ProduceFlag.WEAK)) {
                    if (included.add(stepBuilder)) {
                        // recursively add
                        toAdd.addLast(stepBuilder);
                    }
                }
                dependencies.add(produce);
                modified = true;
            }
        }
        if (modified) {
            // someone has produced this item non-overridably
            return;
        }
        for (Produce produce : allProduces.getOrDefault(idToAdd, Collections.emptyList())) {
            final BuildStepBuilder stepBuilder = produce.getStepBuilder();
            // if overridable, add in this pass only if the first pass didn't add any producers
            if (produce.getFlags().contains(ProduceFlag.OVERRIDABLE)) {
                if (!produce.getFlags().contains(ProduceFlag.WEAK)) {
                    if (included.add(stepBuilder)) {
                        // recursively add
                        toAdd.addLast(stepBuilder);
                    }
                }
                dependencies.add(produce);
            }
        }
    }

    private StepInfo buildOne(BuildStepBuilder toBuild, Set<BuildStepBuilder> included, Map<BuildStepBuilder, StepInfo> mapped,
            Map<BuildStepBuilder, Set<BuildStepBuilder>> dependents, Map<BuildStepBuilder, Set<Produce>> dependencies,
            final Set<StepInfo> startSteps, final Set<StepInfo> endSteps) {
        if (mapped.containsKey(toBuild)) {
            return mapped.get(toBuild);
        }
        Set<StepInfo> dependentStepInfos = new HashSet<>();
        final Set<BuildStepBuilder> dependentsOfThis = dependents.getOrDefault(toBuild, Collections.emptySet());
        for (BuildStepBuilder dependentBuilder : dependentsOfThis) {
            if (included.contains(dependentBuilder)) {
                dependentStepInfos
                        .add(buildOne(dependentBuilder, included, mapped, dependents, dependencies, startSteps, endSteps));
            }
        }
        final Set<Produce> dependenciesOfThis = dependencies.getOrDefault(toBuild, Collections.emptySet());
        int includedDependencies = 0;
        final Set<BuildStepBuilder> visited = new HashSet<>();
        for (Produce produce : dependenciesOfThis) {
            final BuildStepBuilder stepBuilder = produce.getStepBuilder();
            if (included.contains(stepBuilder) && visited.add(stepBuilder)) {
                includedDependencies++;
            }
        }
        int includedDependents = 0;
        for (BuildStepBuilder dependent : dependentsOfThis) {
            if (included.contains(dependent)) {
                includedDependents++;
            }
        }
        final StepInfo stepInfo = new StepInfo(toBuild, includedDependencies, dependentStepInfos);
        mapped.put(toBuild, stepInfo);
        if (includedDependencies == 0) {
            // it's a start step!
            startSteps.add(stepInfo);
        }
        if (includedDependents == 0) {
            // it's an end step!
            endSteps.add(stepInfo);
        }
        return stepInfo;
    }

    void addProvider(final BuildProvider provider) throws ChainBuildException {
        providers.add(provider);
        provider.installInto(this);
    }

    void addStep(final BuildStepBuilder stepBuilder, final StackTraceElement[] stackTrace) {
        if (stepBuilder.getBuildStep() == null) {
            throw new IllegalArgumentException("Null build step");
        }
        steps.put(stepBuilder, stackTrace);
    }

    BuildStepBuilder getFinalStep() {
        return finalStep;
    }

    List<BuildProvider> getProviders() {
        return providers;
    }

    Map<BuildStepBuilder, StackTraceElement[]> getSteps() {
        return steps;
    }

    Set<ItemId> getInitialIds() {
        return initialIds;
    }

    Set<ItemId> getFinalIds() {
        return finalIds;
    }
}
