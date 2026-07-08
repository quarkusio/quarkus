package io.quarkus.deployment.steps;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.core.deployment.action.impl.TransliteratedAction;
import io.quarkus.deployment.builditem.MainBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.StaticBytecodeRecorderBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;

/**
 * Builds a topologically-sorted graph of service nodes from build items
 * and the build step dependency graph.
 * <p>
 * This is a build-time helper used by {@link MainClassBuildStep} to compute
 * the runtime service graph structure. The output is a {@link GraphPlan}
 * containing {@link NodeDescriptor}s in topological order, ready for
 * Gizmo code generation.
 * <p>
 * The algorithm:
 * <ol>
 * <li>Group build items by producing step ID</li>
 * <li>Create logical nodes: legacy recorder chunks (one per step)
 * and individual service nodes (one per TransliteratedAction)</li>
 * <li>Resolve edges from the step dependency graph, skipping
 * passthrough steps (those with no recorder/service output)</li>
 * <li>Add explicit service dependency edges from {@code .after()}
 * declarations</li>
 * <li>Topological sort</li>
 * <li>Compute dependent counts, identify roots and leaves</li>
 * </ol>
 */
final class ServiceGraphBuilder {

    private ServiceGraphBuilder() {
    }

    // ═══════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════

    /**
     * Build a graph plan for the static-init phase.
     *
     * @param items the static-init build items
     * @param stepGraph the step dependency graph (stepId → dependency stepIds)
     * @return the graph plan
     */
    static GraphPlan buildStaticInit(
            List<StaticBytecodeRecorderBuildItem> items,
            Map<String, Set<String>> stepGraph,
            Map<String, Set<String>> buildItemProducers) {
        List<ItemEntry> entries = new ArrayList<>(items.size());
        for (StaticBytecodeRecorderBuildItem item : items) {
            entries.add(new ItemEntry(item.getStepId(), item.getBytecodeRecorder(), item.getTransliteratedAction()));
        }
        return build(entries, stepGraph, Set.of(), buildItemProducers);
    }

    /**
     * Build a graph plan for the runtime-init phase.
     *
     * @param items the runtime-init build items
     * @param stepGraph the step dependency graph (stepId → dependency stepIds)
     * @param staticInitServiceKeys service keys from the static-init phase (for cross-phase proxies)
     * @param buildItemProducers build item class name → producing step IDs
     * @return the graph plan
     */
    static GraphPlan buildRuntime(
            List<MainBytecodeRecorderBuildItem> items,
            Map<String, Set<String>> stepGraph,
            Set<String> staticInitServiceKeys,
            Map<String, Set<String>> buildItemProducers) {
        List<ItemEntry> entries = new ArrayList<>(items.size());
        for (MainBytecodeRecorderBuildItem item : items) {
            entries.add(new ItemEntry(item.getStepId(), item.getBytecodeRecorder(), item.getTransliteratedAction()));
        }
        return build(entries, stepGraph, staticInitServiceKeys, buildItemProducers);
    }

    /**
     * Build a graph plan for the runtime-init phase (no cross-phase keys or build item producers).
     * Used only by tests.
     */
    static GraphPlan buildRuntime(
            List<MainBytecodeRecorderBuildItem> items,
            Map<String, Set<String>> stepGraph) {
        return buildRuntime(items, stepGraph, Set.of(), Map.of());
    }

    // ═══════════════════════════════════════════════
    // Internal: unified entry for both phases
    // ═══════════════════════════════════════════════

    /**
     * A phase-neutral build item entry.
     *
     * @param stepId the producing step's ID
     * @param recorder the legacy recorder, or {@code null}
     * @param action the transliterated action, or {@code null}
     */
    private record ItemEntry(String stepId, BytecodeRecorderImpl recorder, TransliteratedAction action) {
    }

    // ═══════════════════════════════════════════════
    // Core algorithm
    // ═══════════════════════════════════════════════

    private static GraphPlan build(List<ItemEntry> entries, Map<String, Set<String>> stepGraph,
            Set<String> crossPhaseServiceKeys, Map<String, Set<String>> buildItemProducers) {
        // Step 1: group by step ID, preserving order within each step
        Map<String, List<ItemEntry>> byStep = new LinkedHashMap<>();
        for (ItemEntry entry : entries) {
            byStep.computeIfAbsent(entry.stepId(), k -> new ArrayList<>()).add(entry);
        }

        // Step 2: create logical nodes
        List<MutableNode> nodes = new ArrayList<>();
        Map<String, List<MutableNode>> stepToNodes = new HashMap<>();
        Map<String, MutableNode> serviceKeyToNode = new HashMap<>();

        for (var mapEntry : byStep.entrySet()) {
            String stepId = mapEntry.getKey();
            List<ItemEntry> stepEntries = mapEntry.getValue();

            // separate legacy recorders from transliterated actions
            List<BytecodeRecorderImpl> recorders = new ArrayList<>();
            List<TransliteratedAction> actions = new ArrayList<>();
            for (ItemEntry entry : stepEntries) {
                if (entry.recorder() != null) {
                    recorders.add(entry.recorder());
                }
                if (entry.action() != null) {
                    actions.add(entry.action());
                }
            }

            MutableNode recorderNode = null;
            if (!recorders.isEmpty()) {
                recorderNode = new MutableNode(stepId + "/recorder", stepId,
                        NodeKind.LEGACY_RECORDER, recorders, null);
                nodes.add(recorderNode);
                stepToNodes.computeIfAbsent(stepId, k -> new ArrayList<>()).add(recorderNode);
            }

            for (TransliteratedAction action : actions) {
                MutableNode serviceNode = new MutableNode(action.serviceKey(), stepId,
                        kindForAction(action), null, action);
                // no implicit intra-step ordering: services are independent from
                // recorders in the same step. Use afterBuildItem(), after(), or
                // require() to declare explicit ordering dependencies.
                nodes.add(serviceNode);
                stepToNodes.computeIfAbsent(stepId, k -> new ArrayList<>()).add(serviceNode);
                serviceKeyToNode.put(action.serviceKey(), serviceNode);
            }
        }

        if (nodes.isEmpty()) {
            return GraphPlan.EMPTY;
        }

        // Step 3: resolve step-graph edges (skip passthrough steps)
        // Only nodes that interact with the StartupContext maps use step-graph
        // ordering: LEGACY_RECORDER (reads/writes values map) and ALIAS (reads
        // from values map via recorder proxy key). Pure service nodes get their
        // ordering from declared service dependencies (require/after/before)
        // and intra-step ordering (service depends on recorder in same step).
        // This avoids pulling in irrelevant build-time ordering edges for pure
        // service steps (which would cause spurious cycles with before() edges).
        Set<String> stepsWithNodes = stepToNodes.keySet();
        Map<String, Set<MutableNode>> passthroughCache = new HashMap<>();

        for (MutableNode node : nodes) {
            if (node.kind != NodeKind.LEGACY_RECORDER && node.kind != NodeKind.ALIAS) {
                continue;
            }
            Set<MutableNode> resolved = resolveStepDeps(
                    node.stepId, stepGraph, stepToNodes, stepsWithNodes, passthroughCache);
            node.stepDeps.addAll(resolved);
        }

        // Step 4: add explicit service dependency edges
        // These go into actionDeps (in declaration order) for stable indexed access.
        // Config-direct deps do not produce graph edges (resolved from SmallRye Config).
        // ConsumeAll deps expand to multiple consecutive actionDeps (one per matching
        // service, sorted by name) for indexed multi-value access.
        var nodeIter = nodes.listIterator();
        while (nodeIter.hasNext()) {
            MutableNode node = nodeIter.next();
            if (node.action instanceof TransliteratedAction.ActionService as) {
                for (var dep : as.dependencies()) {
                    if (dep.configDirect()) {
                        continue;
                    }
                    if (dep.consumeAll()) {
                        // expand consumeAll: find all matching nodes by type prefix,
                        // sorted by service name, and add as consecutive actionDeps
                        String prefix = dep.keyPrefix();
                        List<MutableNode> matches = new ArrayList<>();
                        for (var entry : serviceKeyToNode.entrySet()) {
                            String key = entry.getKey();
                            if (key.startsWith(prefix) && key.length() > prefix.length()) {
                                matches.add(entry.getValue());
                            }
                        }
                        // sort by service name (the part after the prefix)
                        matches.sort(Comparator.comparing(m -> m.name.substring(prefix.length())));
                        node.actionDeps.addAll(matches);
                        continue;
                    }
                    MutableNode target = serviceKeyToNode.get(dep.key());
                    if (target != null) {
                        if (dep.injected()) {
                            // injected deps go in actionDeps (indexed access in deploy method)
                            node.actionDeps.add(target);
                        } else {
                            // ordering-only deps (from after()) go in stepDeps
                            node.stepDeps.add(target);
                        }
                    } else if (dep.injected() && dep.optional()) {
                        // optional injected dep with no matching service:
                        // create an absent placeholder node that completes with
                        // null value, maintaining index alignment with
                        // LambdaTransliterator's graphDepIndex
                        MutableNode absent = new MutableNode(
                                "<<absent:" + dep.key() + ">>", node.stepId,
                                NodeKind.SENTINEL, null, null);
                        nodeIter.add(absent);
                        node.actionDeps.add(absent);
                    } else if (dep.injected()) {
                        if (crossPhaseServiceKeys.contains(dep.key())) {
                            // cross-phase dependency: the service was produced in static-init
                            // and its value is available in the serviceValues map; create a
                            // synthetic proxy node that reads from the map at runtime
                            MutableNode proxy = new MutableNode(
                                    dep.key(), node.stepId,
                                    NodeKind.CROSS_PHASE_PROXY, null, null);
                            nodeIter.add(proxy);
                            serviceKeyToNode.put(dep.key(), proxy);
                            node.actionDeps.add(proxy);
                        } else {
                            throw new IllegalStateException(
                                    "Service '" + node.name + "' requires '" + dep.key()
                                            + "' but no service with that key exists in the "
                                            + "graph. Check that the producing extension is present "
                                            + "and the service key matches.");
                        }
                    }
                }
            } else if (node.action instanceof TransliteratedAction.RuntimeValueWrapper rvw) {
                // RV_WRAPPER reads the source value via indexed dependency access (dep 0)
                MutableNode source = serviceKeyToNode.get(rvw.sourceServiceKey());
                if (source != null) {
                    node.actionDeps.add(source);
                }
            }
        }

        // Step 4b: add reverse dependency edges (.before() declarations)
        // A before(X) declaration means X depends on this service (reverse of after).
        // This service starts before X and stops after X.
        for (MutableNode node : nodes) {
            if (node.action instanceof TransliteratedAction.ActionService as) {
                for (String targetKey : as.beforeKeys()) {
                    MutableNode target = serviceKeyToNode.get(targetKey);
                    if (target != null) {
                        target.stepDeps.add(node);
                    }
                    // silently ignored if target doesn't exist (optional relationship)
                }
            }
        }

        // Step 4c: resolve afterBuildItem() declarations
        // For each service that declares afterBuildItem(X), find the step(s)
        // that produce X and resolve to their nodes (with passthrough
        // resolution for steps that have no nodes in the service graph).
        for (MutableNode node : nodes) {
            if (node.action instanceof TransliteratedAction.ActionService as) {
                for (String buildItemClass : as.afterBuildItemClasses()) {
                    Set<String> producerSteps = buildItemProducers.getOrDefault(buildItemClass, Set.of());
                    for (String producerStepId : producerSteps) {
                        List<MutableNode> producerNodes = stepToNodes.getOrDefault(producerStepId, List.of());
                        if (!producerNodes.isEmpty()) {
                            node.stepDeps.addAll(producerNodes);
                        } else {
                            // producer step has no nodes — resolve through its
                            // dependencies (passthrough) until we find steps with nodes
                            Set<MutableNode> resolved = resolveStepDeps(
                                    producerStepId, stepGraph, stepToNodes, stepsWithNodes, passthroughCache);
                            node.stepDeps.addAll(resolved);
                        }
                    }
                }
            }
        }

        // Step 5: topological sort
        List<MutableNode> sorted = topologicalSort(nodes);

        // Step 6: assign plan indices and compute dependent counts
        int topIndex = 0;
        for (int i = 0; i < sorted.size(); i++) {
            sorted.get(i).planIndex = i + 1; // offset by 1 for top sentinel
        }
        int bottomIndex = sorted.size() + 1;

        for (MutableNode node : sorted) {
            for (MutableNode dep : node.allDeps()) {
                dep.dependentCount++;
            }
        }

        List<MutableNode> roots = new ArrayList<>();
        List<MutableNode> leaves = new ArrayList<>();
        for (MutableNode node : sorted) {
            if (node.allDeps().isEmpty()) {
                roots.add(node);
            }
            if (node.dependentCount == 0) {
                leaves.add(node);
            }
        }

        // Step 7: build the output plan
        List<NodeDescriptor> descriptors = new ArrayList<>(sorted.size() + 2);

        // top sentinel: roots.size() dependents
        descriptors.add(new NodeDescriptor("<<top>>", NodeKind.SENTINEL, null, null, new int[0], roots.size()));

        for (int i = 0; i < sorted.size(); i++) {
            MutableNode node = sorted.get(i);
            List<MutableNode> allDeps = node.allDeps();
            // resolve dependency indices (action deps first, then step deps)
            int[] depIndices;
            if (allDeps.isEmpty()) {
                // root node: depends on top sentinel only
                depIndices = new int[] { topIndex };
            } else {
                depIndices = new int[allDeps.size()];
                for (int j = 0; j < allDeps.size(); j++) {
                    depIndices[j] = allDeps.get(j).planIndex;
                }
            }
            // dependent count: base count + 1 if this is a leaf (for bottom sentinel)
            int depCount = node.dependentCount + (leaves.contains(node) ? 1 : 0);

            descriptors.add(new NodeDescriptor(node.name, node.kind, node.recorders, node.action, depIndices, depCount));
        }

        // bottom sentinel: depends on all leaves, 0 dependents
        int[] leafIndices = new int[leaves.size()];
        for (int i = 0; i < leaves.size(); i++) {
            leafIndices[i] = leaves.get(i).planIndex;
        }
        descriptors.add(new NodeDescriptor("<<bottom>>", NodeKind.SENTINEL, null, null, leafIndices, 0));

        return new GraphPlan(descriptors, topIndex, bottomIndex, Set.copyOf(serviceKeyToNode.keySet()));
    }

    // ═══════════════════════════════════════════════
    // Step-graph edge resolution with passthrough skipping
    // ═══════════════════════════════════════════════

    /**
     * Resolve the step-graph dependencies for a given step, skipping
     * passthrough steps (those without recorder/service nodes) by
     * recursively following their dependencies.
     *
     * @param stepId the step to resolve
     * @param stepGraph the full step dependency graph
     * @param stepToNodes map of step IDs that have nodes
     * @param stepsWithNodes the set of step IDs that have nodes
     * @param cache memoization cache for passthrough resolution
     * @return the set of nodes that this step depends on
     */
    private static Set<MutableNode> resolveStepDeps(
            String stepId,
            Map<String, Set<String>> stepGraph,
            Map<String, List<MutableNode>> stepToNodes,
            Set<String> stepsWithNodes,
            Map<String, Set<MutableNode>> cache) {
        Set<String> depStepIds = stepGraph.getOrDefault(stepId, Set.of());
        Set<MutableNode> result = new HashSet<>();
        for (String depStepId : depStepIds) {
            if (stepsWithNodes.contains(depStepId)) {
                result.addAll(stepToNodes.get(depStepId));
            } else {
                // passthrough step: resolve transitively
                result.addAll(resolvePassthrough(depStepId, stepGraph, stepToNodes, stepsWithNodes, cache));
            }
        }
        return result;
    }

    /**
     * Recursively resolve a passthrough step's dependencies.
     * Results are memoized.
     */
    private static Set<MutableNode> resolvePassthrough(
            String stepId,
            Map<String, Set<String>> stepGraph,
            Map<String, List<MutableNode>> stepToNodes,
            Set<String> stepsWithNodes,
            Map<String, Set<MutableNode>> cache) {
        Set<MutableNode> cached = cache.get(stepId);
        if (cached != null) {
            return cached;
        }
        // mark as in-progress (empty) to handle cycles
        Set<MutableNode> result = new HashSet<>();
        cache.put(stepId, result);

        Set<String> depStepIds = stepGraph.getOrDefault(stepId, Set.of());
        for (String depStepId : depStepIds) {
            if (stepsWithNodes.contains(depStepId)) {
                result.addAll(stepToNodes.get(depStepId));
            } else {
                result.addAll(resolvePassthrough(depStepId, stepGraph, stepToNodes, stepsWithNodes, cache));
            }
        }
        return result;
    }

    // ═══════════════════════════════════════════════
    // Topological sort (Kahn's algorithm)
    // ═══════════════════════════════════════════════

    private static List<MutableNode> topologicalSort(List<MutableNode> nodes) {
        // build reverse adjacency and compute in-degree using node fields
        // (avoids Map<MutableNode, Integer>)
        Map<MutableNode, List<MutableNode>> reverseAdj = new HashMap<>();
        for (MutableNode node : nodes) {
            List<MutableNode> allDeps = node.allDeps();
            node.inDegree = 0;
            for (MutableNode dep : allDeps) {
                node.inDegree++;
                reverseAdj.computeIfAbsent(dep, k -> new ArrayList<>()).add(node);
            }
        }

        Deque<MutableNode> queue = new ArrayDeque<>();
        for (MutableNode node : nodes) {
            if (node.inDegree == 0) {
                queue.add(node);
            }
        }

        List<MutableNode> sorted = new ArrayList<>(nodes.size());
        while (!queue.isEmpty()) {
            MutableNode node = queue.poll();
            sorted.add(node);
            for (MutableNode dependent : reverseAdj.getOrDefault(node, List.of())) {
                if (--dependent.inDegree == 0) {
                    queue.add(dependent);
                }
            }
        }

        if (sorted.size() != nodes.size()) {
            // identify the nodes stuck in the cycle (inDegree > 0 after sort)
            List<String> cycleNodes = new ArrayList<>();
            for (MutableNode node : nodes) {
                if (node.inDegree > 0) {
                    List<String> depNames = node.allDeps().stream()
                            .filter(d -> d.inDegree > 0)
                            .map(d -> d.name)
                            .toList();
                    cycleNodes.add(node.name + " → " + depNames);
                }
            }
            throw new IllegalStateException(
                    "Service dependency graph contains a cycle (" + sorted.size() + " of " + nodes.size()
                            + " nodes sorted). Unsorted nodes:\n  " + String.join("\n  ", cycleNodes));
        }
        return sorted;
    }

    // ═══════════════════════════════════════════════
    // Internal types
    // ═══════════════════════════════════════════════

    private static NodeKind kindForAction(TransliteratedAction action) {
        return switch (action) {
            case TransliteratedAction.ActionService a -> NodeKind.SERVICE;
            case TransliteratedAction.AliasService a -> NodeKind.ALIAS;
            case TransliteratedAction.RuntimeValueWrapper a -> NodeKind.RV_WRAPPER;
        };
    }

    /**
     * Mutable node used during graph construction.
     */
    private static final class MutableNode {
        final String name;
        final String stepId;
        final NodeKind kind;
        final List<BytecodeRecorderImpl> recorders; // non-null for LEGACY_RECORDER
        final TransliteratedAction action; // non-null for SERVICE/ALIAS/RV_WRAPPER

        /** Action-declared dependencies (from .after()), in declaration order. */
        final List<MutableNode> actionDeps = new ArrayList<>();
        /** Step-graph-derived dependencies (ordering edges). */
        final Set<MutableNode> stepDeps = new HashSet<>();
        // computed during toposort / step 6
        int inDegree;
        int dependentCount;
        int planIndex = -1;

        MutableNode(String name, String stepId, NodeKind kind,
                List<BytecodeRecorderImpl> recorders, TransliteratedAction action) {
            this.name = name;
            this.stepId = stepId;
            this.kind = kind;
            this.recorders = recorders;
            this.action = action;
        }

        /**
         * Get all dependencies (action deps first, then step deps not already in action deps).
         *
         * @return combined dependency list with action deps at stable positions
         */
        List<MutableNode> allDeps() {
            if (stepDeps.isEmpty()) {
                return actionDeps;
            }
            if (actionDeps.isEmpty()) {
                return new ArrayList<>(stepDeps);
            }
            List<MutableNode> result = new ArrayList<>(actionDeps);
            for (MutableNode sd : stepDeps) {
                if (!actionDeps.contains(sd)) {
                    result.add(sd);
                }
            }
            return result;
        }
    }

    // ═══════════════════════════════════════════════
    // Output types
    // ═══════════════════════════════════════════════

    /**
     * The kind of service node.
     */
    enum NodeKind {
        /** Top or bottom sentinel. */
        SENTINEL,
        /** Legacy bytecode recorder chunk (one or more recorders from the same step). */
        LEGACY_RECORDER,
        /** New-style service with a transliterated lambda body. */
        SERVICE,
        /** Alias that copies a recorder proxy value to a service key. */
        ALIAS,
        /** Wrapper that wraps a service value in a RuntimeValue. */
        RV_WRAPPER,
        /** Synthetic proxy that reads a cross-phase service value from the serviceValues map. */
        CROSS_PHASE_PROXY,
    }

    /**
     * Descriptor for a single node in the graph plan.
     * Indices in {@code dependencyIndices} refer to positions in the
     * {@link GraphPlan#nodes()} list.
     *
     * @param name diagnostic name
     * @param kind the node kind
     * @param recorders legacy recorders (only for {@link NodeKind#LEGACY_RECORDER})
     * @param action the transliterated action (only for SERVICE/ALIAS/RV_WRAPPER)
     * @param dependencyIndices indices of dependency nodes in the plan's node list
     * @param dependentCount the number of dependents this node will have
     */
    record NodeDescriptor(
            String name,
            NodeKind kind,
            List<BytecodeRecorderImpl> recorders,
            TransliteratedAction action,
            int[] dependencyIndices,
            int dependentCount) {
    }

    /**
     * The complete graph construction plan for one phase.
     * Nodes are in topological order with sentinels at the ends.
     * Index 0 is always the top sentinel; the last index is always
     * the bottom sentinel.
     *
     * @param nodes the node descriptors in topological order
     * @param topIndex index of the top sentinel (always 0)
     * @param bottomIndex index of the bottom sentinel (always nodes.size() - 1)
     */
    record GraphPlan(List<NodeDescriptor> nodes, int topIndex, int bottomIndex, Set<String> serviceKeys) {

        static final GraphPlan EMPTY = new GraphPlan(List.of(), -1, -1, Set.of());

        /**
         * Return whether this plan has any service nodes.
         *
         * @return {@code true} if the plan is non-empty
         */
        boolean hasNodes() {
            return !nodes.isEmpty();
        }
    }
}
