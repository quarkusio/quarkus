package io.quarkus.builder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.builder.item.BuildItem;

final class ResortOrdinals {

    // static final String LOGGING_SETUP_STEP = "io.quarkus.deployment.logging.LoggingResourceProcessor#setupLoggingRuntimeInit";
    private static final String RECORDER_BUILD_ITEM = "io.quarkus.deployment.builditem.MainBytecodeRecorderBuildItem";

    static void mutate(ClassLoader classLoader, Set<StepInfo> startSteps, Map<ItemId, int[]> producingOrdinals) {
        Map<StepInfo, Set<StepInfo>> dependencies = computeMainBytecodeRecorderDependencies(startSteps);
        Class<? extends BuildItem> bytecodeClazz;
        try {
            bytecodeClazz = classLoader.loadClass(RECORDER_BUILD_ITEM)
                    .asSubclass(BuildItem.class);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        if (dependencies.isEmpty()) {
            return;
        }

        // find logging step
        String loggingStepId = "io.quarkus.deployment.logging.LoggingResourceProcessor#setupLoggingRuntimeInit";
        StepInfo loggingStep = dependencies.keySet().stream()
                .filter(s -> loggingStepId.equals(s.getBuildStep().getId()))
                .findFirst()
                .orElse(null);

        if (loggingStep == null) {
            return;
        }

        Set<StepInfo> recursiveDeps = new HashSet<>();
        Deque<StepInfo> toVisit = new ArrayDeque<>();

        // Start traversal from direct dependencies of the logging step
        Set<StepInfo> directDeps = dependencies.get(loggingStep);
        if (directDeps != null) {
            for (StepInfo dep : directDeps) {
                if (recursiveDeps.add(dep)) {
                    toVisit.push(dep);
                }
            }
        }

        // Depth-first search to find all transitive dependencies
        while (!toVisit.isEmpty()) {
            StepInfo current = toVisit.pop();
            Set<StepInfo> currentDeps = dependencies.get(current);
            if (currentDeps != null) {
                for (StepInfo dep : currentDeps) {
                    if (recursiveDeps.add(dep)) {
                        toVisit.push(dep);
                    }
                }
            }
        }

        List<StepInfo> sortedLogDeps = new ArrayList<>(recursiveDeps);
        sortedLogDeps.add(loggingStep);
        sortedLogDeps.sort(Comparator.comparingInt(StepInfo::getOrdinal));

        var ordinals2 = dependencies.keySet().stream()
                .sorted(Comparator.comparingInt(StepInfo::getOrdinal))
                .toList();

        List<Integer> availableSlots = ordinals2.stream()
                .map(StepInfo::getOrdinal)
                .sorted()
                .toList();
        List<StepInfo> newSequence = new ArrayList<>(sortedLogDeps);
        List<StepInfo> remainder = ordinals2.stream()
                .filter(s -> !newSequence.contains(s))
                .toList();
        newSequence.addAll(remainder);

        var changeInOrdinals = new HashMap<Integer, Integer>();

        for (int i = 0; i < newSequence.size(); i++) {
            var a = newSequence.get(i).getOrdinal();
            var b = availableSlots.get(i);
            if (a != b) {
                changeInOrdinals.put(newSequence.get(i).getOrdinal(), availableSlots.get(i));
                newSequence.get(i).setOrdinal(availableSlots.get(i));
            }
        }

        newSequence.stream().map(StepInfo::getProduces)
                .flatMap(Set::stream)
                .filter(itemId -> !itemId.getType().equals(bytecodeClazz) && itemId.isMulti())
                .distinct()
                .forEach(itemId -> {
                    int[] ordinals = producingOrdinals.get(itemId);
                    var modified = false;
                    for (int i = 0; i < ordinals.length; i++) {
                        if (changeInOrdinals.containsKey(ordinals[i])) {
                            // Is it ok to do that? What if order of build items in the list matters?
                            ordinals[i] = changeInOrdinals.get(ordinals[i]);
                            modified = true;
                        }
                    }
                    if (modified) {
                        // resort
                        Arrays.sort(ordinals);
                    }
                });

        Map<StepInfo, Set<StepInfo>> dependencies2 = computeMainBytecodeRecorderDependencies(startSteps);
        var sortedEndResult = dependencies2.keySet().stream().sorted(Comparator.comparingInt(StepInfo::getOrdinal)).toList();

        System.out.println("---- Logging Setup Dependencies (Topological) ----");
        // for (StepInfo step : sortedLogDeps) {
        //     System.out.println(step.getBuildStep().getId());
        // }
        // System.out.println("--------------------------------------------------");
    }

    private static Map<StepInfo, Set<StepInfo>> computeMainBytecodeRecorderDependencies(Set<StepInfo> startSteps) {
        Set<StepInfo> allSteps = collectAllSteps(startSteps);
        Map<StepInfo, Set<StepInfo>> reverseDeps = buildReverseDeps(allSteps);

        Set<StepInfo> producers = new HashSet<>();
        for (StepInfo step : allSteps) {
            for (ItemId item : step.getProduces()) {
                if (RECORDER_BUILD_ITEM.equals(item.getType().getName())) {
                    producers.add(step);
                    break;
                }
            }
        }

        Map<StepInfo, Set<StepInfo>> result = new HashMap<>();

        for (StepInfo producer : producers) {
            Set<StepInfo> dependencies = new HashSet<>();
            Deque<StepInfo> stack = new ArrayDeque<>();
            Set<StepInfo> visited = new HashSet<>();

            Set<StepInfo> directDeps = reverseDeps.get(producer);
            if (directDeps != null) {
                for (StepInfo dep : directDeps) {
                    stack.push(dep);
                    visited.add(dep);
                }
            }

            while (!stack.isEmpty()) {
                StepInfo current = stack.pop();
                if (producers.contains(current)) {
                    dependencies.add(current);
                }

                Set<StepInfo> currentDeps = reverseDeps.get(current);
                if (currentDeps != null) {
                    for (StepInfo dep : currentDeps) {
                        if (visited.add(dep)) {
                            stack.push(dep);
                        }
                    }
                }
            }
            // Note: dependencies may be empty!
            result.put(producer, dependencies);
        }

        return result;
    }

    private static Set<StepInfo> collectAllSteps(Set<StepInfo> startSteps) {
        Set<StepInfo> all = new HashSet<>();
        Deque<StepInfo> stack = new ArrayDeque<>(startSteps);

        while (!stack.isEmpty()) {
            StepInfo step = stack.pop();
            if (all.add(step)) {
                stack.addAll(step.getDependents());
            }
        }
        return all;
    }

    private static Map<StepInfo, Set<StepInfo>> buildReverseDeps(Set<StepInfo> allSteps) {
        Map<StepInfo, Set<StepInfo>> reverse = new HashMap<>();

        for (StepInfo step : allSteps) {
            for (StepInfo dependent : step.getDependents()) {
                reverse.computeIfAbsent(dependent, k -> new HashSet<>())
                        .add(step);
            }
        }
        return reverse;
    }

}
