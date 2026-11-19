package io.quarkus.kubernetes.deployment;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.kubernetes.spi.KubernetesEnvBuildItem;

/**
 * Validates that the set of provided environment variables is valid.
 */
public class EnvVarValidator {
    private static final Logger log = Logger.getLogger(EnvVarValidator.class);
    private static final Pattern ENV_VAR_REFERENCE = Pattern.compile("(?<!\\$)\\$\\(([A-Z_][A-Z0-9_]*)\\)");

    private final Map<ItemKey, KubernetesEnvBuildItem> items = new LinkedHashMap<>();
    private final Set<String> knownNames = new HashSet<>();
    private final Map<String, Set<KubernetesEnvBuildItem>> conflicting = new HashMap<>();
    private final Set<String> errors = new HashSet<>();

    /**
     * Processes the specified {@link KubernetesEnvBuildItem} to check whether it's valid with respect to the set of already
     * known configuration, accumulating errors or outputting warnings if needed.
     *
     * @param item the {@link KubernetesEnvBuildItem} to validate
     */
    void process(KubernetesEnvBuildItem item) {
        final String name = item.getName();
        final ShouldAddHolder wrapper = new ShouldAddHolder();
        // check if we already have defined a build item with the same name
        if (knownNames.contains(name)) {
            final KubernetesEnvBuildItem.EnvType type = item.getType();
            // as the item might not get added, reset the wrapper's state
            wrapper.setShouldAdd(false);
            // then go through already added items to check if the item needs to be added, warning logged or error added
            items.values().stream()
                    .filter(kebi -> name.equals(kebi.getName()))
                    .forEach(existing -> {
                        final KubernetesEnvBuildItem.EnvType existingType = existing.getType();
                        // if the type doesn't allow multiple definitions, we need to check if we're adding a "compatible" env var
                        if (!existingType.allowMultipleDefinitions) {
                            // check if we have a conflict and thus an error
                            if (existingType.mightConflictWith(type)) {
                                addError(item, existing);
                            } else {
                                // we're not dealing with a potentially conflicting var so add the new item
                                wrapper.setShouldAdd(true);
                            }
                        } else {
                            if (existingType.mightConflictWith(type)) {
                                log.warn("Ignoring duplicate definition of " + item);
                            }
                            wrapper.setShouldAdd(true);

                        }
                    });
        }
        if (wrapper.shouldAdd()) {
            items.put(ItemKey.keyFor(item), item);
        }
        knownNames.add(name);
    }

    private void addError(KubernetesEnvBuildItem item, KubernetesEnvBuildItem existing) {
        final Set<KubernetesEnvBuildItem> inError = conflicting.computeIfAbsent(item.getName(), k -> new LinkedHashSet<>());
        inError.add(existing);
        inError.add(item);
    }

    /**
     * Retrieves a collection of validated {@link KubernetesEnvBuildItem} once all of them have been processed.
     *
     * @return a collection of validated {@link KubernetesEnvBuildItem}
     * @throws IllegalArgumentException if the processed items result in an invalid configuration
     */
    Collection<KubernetesEnvBuildItem> getBuildItems() {
        if (conflicting.isEmpty() && errors.isEmpty()) {
            return sortByDependencies(items);
        }
        throw new IllegalArgumentException(getError());
    }

    private Collection<KubernetesEnvBuildItem> sortByDependencies(Map<ItemKey, KubernetesEnvBuildItem> items) {
        List<KubernetesEnvBuildItem> envFromItems = new ArrayList<>();
        Map<String, KubernetesEnvBuildItem> namedVars = new LinkedHashMap<>();

        for (KubernetesEnvBuildItem item : items.values()) {
            KubernetesEnvBuildItem.EnvType type = item.getType();

            if (type == KubernetesEnvBuildItem.EnvType.secret ||
                    type == KubernetesEnvBuildItem.EnvType.configmap) {
                envFromItems.add(item);
            } else {
                String name = item.getName();
                namedVars.put(name, item);
            }
        }

        Map<String, Set<String>> dependencies = new HashMap<>();
        for (Map.Entry<String, KubernetesEnvBuildItem> entry : namedVars.entrySet()) {
            Set<String> deps = extractDependencies(entry.getValue(), namedVars.keySet());
            if (!deps.isEmpty()) {
                dependencies.put(entry.getKey(), deps);
            }
        }

        List<KubernetesEnvBuildItem> sortedVars = topologicalSort(namedVars, dependencies);

        List<KubernetesEnvBuildItem> result = new ArrayList<>(envFromItems.size() + sortedVars.size());
        result.addAll(envFromItems);
        result.addAll(sortedVars);

        return result;
    }

    private Set<String> extractDependencies(KubernetesEnvBuildItem item, Set<String> allVarNames) {
        Set<String> deps = new HashSet<>();
        String value = item.getValue();

        if (value != null && !value.isEmpty()) {
            Matcher matcher = ENV_VAR_REFERENCE.matcher(value);
            while (matcher.find()) {
                String depName = matcher.group(1);

                for (String configName : allVarNames) {
                    if (depName.equals(EnvConverter.convertName(configName))) {
                        deps.add(configName);
                    }
                }
            }
        }

        return deps;
    }

    private List<KubernetesEnvBuildItem> topologicalSort(
            Map<String, KubernetesEnvBuildItem> namedVars,
            Map<String, Set<String>> dependencies) {

        Map<String, Integer> inDegree = new HashMap<>();
        for (String name : namedVars.keySet()) {
            inDegree.put(name, 0);
        }

        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            for (String dep : entry.getValue()) {
                if (namedVars.containsKey(dep)) {
                    inDegree.merge(entry.getKey(), 1, Integer::sum);
                }
            }
        }

        PriorityQueue<String> queue = new PriorityQueue<>();

        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        List<KubernetesEnvBuildItem> sorted = new ArrayList<>();

        while (!queue.isEmpty()) {
            String current = queue.poll();
            sorted.add(namedVars.get(current));
            log.debugf("Topological sort: added %s", current);

            for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
                if (entry.getValue().contains(current)) {
                    String dependent = entry.getKey();
                    int newDegree = inDegree.merge(dependent, -1, Integer::sum);
                    if (newDegree == 0) {
                        queue.offer(dependent);
                        log.debugf("Topological sort: %s is now ready (depends on %s)", dependent, current);
                    }
                }
            }
        }

        if (sorted.size() < namedVars.size()) {
            log.warn("Circular dependency detected in environment variables, using insertion order");
            return new ArrayList<>(namedVars.values());
        }

        log.debugf("Topological sort complete: %s",
                sorted.stream().map(KubernetesEnvBuildItem::getName).collect(Collectors.toList()));
        return sorted;
    }

    private String getError() {
        String error = "\n";
        if (!conflicting.isEmpty()) {
            error += "\t+ Conflicts in environment variable definitions:\n";
            error += conflicting.entrySet().stream()
                    .map(e -> {
                        final String conflicting = e.getValue().stream()
                                .map(Object::toString)
                                .collect(Collectors.joining(" redefined as "));
                        return String.format("\t\t- '%s': first defined as %s", e.getKey(), conflicting);
                    })
                    .collect(Collectors.joining("\n"));
        }
        if (!errors.isEmpty()) {
            error += "\t+ Invalid declarations:\n";
            error += errors.stream().map(s -> "\t\t- " + s).collect(Collectors.joining("\n"));
        }
        return error;
    }

    private static final class ShouldAddHolder {
        private boolean shouldAdd;

        ShouldAddHolder() {
            shouldAdd = true;
        }

        public boolean shouldAdd() {
            return shouldAdd;
        }

        public void setShouldAdd(boolean shouldAdd) {
            this.shouldAdd = shouldAdd;
        }
    }

    private static final class ItemKey {
        private final KubernetesEnvBuildItem.EnvType type;
        private final String name;

        ItemKey(KubernetesEnvBuildItem.EnvType type, String name) {
            this.type = type;
            this.name = name;
        }

        public static ItemKey keyFor(KubernetesEnvBuildItem item) {
            return new ItemKey(item.getType(), item.getName());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            ItemKey itemKey = (ItemKey) o;

            if (type != itemKey.type)
                return false;
            return name.equals(itemKey.name);
        }

        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + name.hashCode();
            return result;
        }
    }
}
