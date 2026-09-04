package io.quarkus.deployment.steps;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.core.deployment.action.impl.Dependency;
import io.quarkus.core.deployment.action.impl.ServiceMetadataBuildItem;
import io.quarkus.core.deployment.action.impl.StaticServiceMetadataBuildItem;
import io.quarkus.deployment.Phase;

/**
 * Validates the service dependency graphs built from {@link ServiceMetadataBuildItem}
 * and {@link StaticServiceMetadataBuildItem} instances.
 * <p>
 * This validator is called from {@link io.quarkus.deployment.QuarkusAugmentor#run()} after
 * all build steps have completed and the metadata items have been collected.
 * <p>
 * Validation is layered:
 * <ol>
 * <li>The static-init graph is validated independently (no pre-existing providers).</li>
 * <li>The runtime graph is validated with static-init outputs as pre-existing providers,
 * allowing runtime services to transparently depend on static-init outputs.</li>
 * <li>Cross-phase duplicate detection rejects any service key registered in both phases.</li>
 * </ol>
 * Within each phase, the following checks are performed:
 * <ul>
 * <li>No duplicate services (same type + name registered more than once)</li>
 * <li>All declared dependencies are satisfied (by same-phase providers or pre-existing providers)</li>
 * <li>No cycles exist in the dependency graph</li>
 * </ul>
 */
public final class ServiceDependencyValidator {

    private ServiceDependencyValidator() {
    }

    /**
     * Uniform accessor for service metadata regardless of phase.
     */
    private interface ServiceMetadata {
        /**
         * Get the service key (type + name identity).
         *
         * @return the service key
         */
        String serviceKey();

        /**
         * Get the declared dependencies.
         *
         * @return the dependency list
         */
        List<Dependency> dependencies();

        /**
         * Get the build step name (for diagnostics).
         *
         * @return the build step name
         */
        String buildStepName();

        /**
         * Get the runtime phase, or {@code null} for static-init services.
         *
         * @return the runtime phase, or {@code null}
         */
        Phase phase();
    }

    /**
     * Validate the service dependency graphs.
     * <p>
     * Performs layered validation: static-init first (independently), then runtime
     * (with static-init outputs as pre-existing providers), then cross-phase duplicate check.
     *
     * @param staticItems the static-init service metadata items
     * @param runtimeItems the runtime service metadata items
     * @throws IllegalStateException if validation fails
     */
    public static void validate(
            List<StaticServiceMetadataBuildItem> staticItems,
            List<ServiceMetadataBuildItem> runtimeItems) {
        if (staticItems.isEmpty() && runtimeItems.isEmpty()) {
            return;
        }

        List<String> errors = new ArrayList<>();

        // adapt static-init items to uniform accessor
        List<ServiceMetadata> staticMeta = new ArrayList<>(staticItems.size());
        for (StaticServiceMetadataBuildItem item : staticItems) {
            staticMeta.add(new ServiceMetadata() {
                public String serviceKey() {
                    return item.serviceKey();
                }

                public List<Dependency> dependencies() {
                    return item.dependencies();
                }

                public String buildStepName() {
                    return item.buildStepName();
                }

                public Phase phase() {
                    return null;
                }
            });
        }

        // adapt runtime items to uniform accessor
        List<ServiceMetadata> runtimeMeta = new ArrayList<>(runtimeItems.size());
        for (ServiceMetadataBuildItem item : runtimeItems) {
            runtimeMeta.add(new ServiceMetadata() {
                public String serviceKey() {
                    return item.serviceKey();
                }

                public List<Dependency> dependencies() {
                    return item.dependencies();
                }

                public String buildStepName() {
                    return item.buildStepName();
                }

                public Phase phase() {
                    return item.phase();
                }
            });
        }

        // phase 1: validate static-init graph independently
        Set<String> staticProviderKeys = validateGraph(staticMeta, Set.of(), "static-init", errors);

        // phase 2: validate runtime graph with static-init outputs as pre-existing providers
        Set<String> runtimeProviderKeys = validateGraph(runtimeMeta, staticProviderKeys, "runtime", errors);

        // phase 3: cross-phase duplicate detection
        for (String key : runtimeProviderKeys) {
            if (staticProviderKeys.contains(key)) {
                errors.add("Cross-phase duplicate: service '" + key
                        + "' is registered in both the static-init and runtime phases");
            }
        }

        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder("Service dependency validation failed:");
            for (String error : errors) {
                sb.append("\n  - ").append(error);
            }
            throw new IllegalStateException(sb.toString());
        }
    }

    /**
     * Validate a single phase's dependency graph.
     * <p>
     * Checks for duplicate registrations within the phase, verifies all dependencies
     * are satisfied (by same-phase providers or pre-existing providers from an earlier phase),
     * and detects cycles within the phase.
     *
     * @param items the service metadata items for this phase
     * @param preExistingProviders service keys from earlier phases that are available as providers
     * @param phaseName the phase name (for error messages)
     * @param errors the error accumulator
     * @return the set of provider keys registered in this phase (excluding pre-existing)
     */
    private static Set<String> validateGraph(
            List<ServiceMetadata> items,
            Set<String> preExistingProviders,
            String phaseName,
            List<String> errors) {
        // build the set of providers for this phase, checking for duplicates
        Map<String, ServiceMetadata> providers = new HashMap<>();
        Set<String> providerKeys = new HashSet<>();

        for (ServiceMetadata item : items) {
            String key = item.serviceKey();
            providerKeys.add(key);
            ServiceMetadata existing = providers.put(key, item);
            if (existing != null) {
                errors.add("Duplicate " + phaseName + " service registration for '" + key
                        + "': registered by build step '" + existing.buildStepName()
                        + "' and also by '" + item.buildStepName() + "'");
            }
        }

        // check that all non-optional dependencies are satisfied (same-phase or pre-existing)
        for (ServiceMetadata item : items) {
            for (Dependency dep : item.dependencies()) {
                if (dep.configDirect()) {
                    continue; // resolved directly from SmallRye Config, not from the service graph
                }
                String depKey = dep.key();
                if (!dep.optional() && !providers.containsKey(depKey) && !preExistingProviders.contains(depKey)) {
                    errors.add("Unsatisfied dependency: " + phaseName + " service '"
                            + item.serviceKey() + "' (build step '" + item.buildStepName()
                            + "') depends on '" + depKey + "' which is not registered");
                }
            }
        }

        // phase ordering validation: a service may only depend on services in the same or earlier phase
        for (ServiceMetadata item : items) {
            Phase itemPhase = item.phase();
            if (itemPhase == null) {
                continue; // static-init services have no phase
            }
            for (Dependency dep : item.dependencies()) {
                if (dep.configDirect()) {
                    continue;
                }
                String depKey;
                if (dep.consumeAll()) {
                    // check each matched provider individually
                    String prefix = dep.keyPrefix();
                    for (Map.Entry<String, ServiceMetadata> entry : providers.entrySet()) {
                        if (entry.getKey().startsWith(prefix)) {
                            Phase depPhase = entry.getValue().phase();
                            if (depPhase != null && depPhase.ordinal() > itemPhase.ordinal()) {
                                errors.add("Phase ordering violation: " + phaseName + " service '"
                                        + item.serviceKey() + "' (phase " + itemPhase
                                        + ", build step '" + item.buildStepName()
                                        + "') depends on '" + entry.getKey()
                                        + "' (phase " + depPhase + ")");
                            }
                        }
                    }
                    continue;
                }
                depKey = dep.key();
                ServiceMetadata depMeta = providers.get(depKey);
                if (depMeta != null) {
                    Phase depPhase = depMeta.phase();
                    if (depPhase != null && depPhase.ordinal() > itemPhase.ordinal()) {
                        errors.add("Phase ordering violation: " + phaseName + " service '"
                                + item.serviceKey() + "' (phase " + itemPhase
                                + ", build step '" + item.buildStepName()
                                + "') depends on '" + depKey
                                + "' (phase " + depPhase + ")");
                    }
                }
            }
        }

        // cycle detection (same-phase edges only; cross-phase deps are acyclic by definition)
        Map<String, List<String>> adjacency = new HashMap<>();
        for (ServiceMetadata item : items) {
            List<String> deps = new ArrayList<>(item.dependencies().size());
            for (Dependency dep : item.dependencies()) {
                if (dep.configDirect()) {
                    continue; // not a service graph edge
                }
                if (dep.consumeAll()) {
                    // consumeAll: add edges to all same-phase providers matching the type prefix
                    String prefix = dep.keyPrefix();
                    for (String providerKey : providers.keySet()) {
                        if (providerKey.startsWith(prefix)) {
                            deps.add(providerKey);
                        }
                    }
                } else {
                    String depKey = dep.key();
                    // only include same-phase edges for cycle detection
                    if (providers.containsKey(depKey)) {
                        deps.add(depKey);
                    }
                }
            }
            adjacency.put(item.serviceKey(), deps);
        }

        Set<String> visited = new HashSet<>();
        Set<String> inStack = new HashSet<>();
        for (String node : adjacency.keySet()) {
            if (!visited.contains(node)) {
                String cycle = detectCycle(node, adjacency, visited, inStack);
                if (cycle != null) {
                    errors.add("Dependency cycle detected in " + phaseName + " graph: " + cycle);
                }
            }
        }

        return providerKeys;
    }

    /**
     * Detect a cycle reachable from the given start node using iterative DFS.
     * Returns a description of the cycle if found, or {@code null} if no cycle exists.
     *
     * @param start the starting node key
     * @param adjacency the adjacency map
     * @param visited the set of fully-explored nodes
     * @param inStack the set of nodes currently on the DFS stack
     * @return a cycle description, or {@code null} if no cycle was found
     */
    private static String detectCycle(
            String start,
            Map<String, List<String>> adjacency,
            Set<String> visited,
            Set<String> inStack) {
        // iterative DFS with explicit stack
        Deque<StackFrame> stack = new ArrayDeque<>();
        stack.push(new StackFrame(start, 0));
        inStack.add(start);

        while (!stack.isEmpty()) {
            StackFrame frame = stack.peek();
            List<String> deps = adjacency.getOrDefault(frame.node, List.of());
            if (frame.index < deps.size()) {
                String dep = deps.get(frame.index);
                frame.index++;
                if (inStack.contains(dep)) {
                    // found a cycle; reconstruct the path
                    StringBuilder path = new StringBuilder(dep);
                    boolean inCycle = false;
                    for (StackFrame f : stack) {
                        if (f.node.equals(dep)) {
                            inCycle = true;
                        }
                        if (inCycle) {
                            path.append(" -> ").append(f.node);
                        }
                    }
                    path.append(" -> ").append(dep);
                    return path.toString();
                }
                if (!visited.contains(dep)) {
                    inStack.add(dep);
                    stack.push(new StackFrame(dep, 0));
                }
            } else {
                visited.add(frame.node);
                inStack.remove(frame.node);
                stack.pop();
            }
        }
        return null;
    }

    /**
     * A DFS stack frame tracking the current node and the index of the next dependency to visit.
     */
    private static final class StackFrame {
        /** The service key of this node. */
        final String node;
        /** The index into the adjacency list for the next edge to explore. */
        int index;

        /**
         * Construct a new instance.
         *
         * @param node the service key
         * @param index the initial edge index
         */
        StackFrame(String node, int index) {
            this.node = node;
            this.index = index;
        }
    }
}
