package io.quarkus.builder;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Public API to expose build step dependency info.
 */
public class StepDependencyInfo {

    final String name;
    final Map<String, Object> attributes = new ConcurrentHashMap<>();
    final Set<StepDependencyInfo> dependents = new HashSet<>();
    final Set<StepDependencyInfo> dependencies = new HashSet<>();

    public StepDependencyInfo(String name) {
        this.name = name;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * Returns all dependents of this step, including indirect dependents.
     *
     */
    public Set<StepDependencyInfo> getDependents() {
        Set<StepDependencyInfo> ret = new HashSet<>();
        Queue<StepDependencyInfo> toProcess = new ArrayDeque<>(dependents);
        while (!toProcess.isEmpty()) {
            StepDependencyInfo i = toProcess.poll();
            if (!ret.contains(i)) {
                ret.add(i);
                toProcess.addAll(i.dependents);
            }
        }
        return ret;
    }

    /**
     * Returns all dependencies of this step, including indirect dependencies.
     */
    public Set<StepDependencyInfo> getDependencies() {
        Set<StepDependencyInfo> ret = new HashSet<>();
        Queue<StepDependencyInfo> toProcess = new ArrayDeque<>(dependencies);
        while (!toProcess.isEmpty()) {
            StepDependencyInfo i = toProcess.poll();
            if (!ret.contains(i)) {
                ret.add(i);
                toProcess.addAll(i.dependencies);
            }
        }
        return ret;
    }

}
