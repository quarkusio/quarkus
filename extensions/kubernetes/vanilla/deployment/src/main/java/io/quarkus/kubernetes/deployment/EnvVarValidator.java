package io.quarkus.kubernetes.deployment;

import java.util.*;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.kubernetes.spi.KubernetesEnvBuildItem;

/**
 * Validates that the set of provided environment variables is valid.
 */
public class EnvVarValidator {
    private static final Logger log = Logger.getLogger(EnvVarValidator.class);
    private final Map<ItemKey, KubernetesEnvBuildItem> items = new HashMap<>();
    private final Set<String> knownNames = new HashSet<>();
    private final Map<String, Set<KubernetesEnvBuildItem>> conflicting = new HashMap<>();
    private final Set<String> errors = new HashSet<>();

    void process(String name, Optional<String> value, Optional<String> secret, Optional<String> configmap,
            Optional<String> field, String target, Optional<String> prefix, boolean... oldStyle) {
        try {
            final KubernetesEnvBuildItem kebi = KubernetesEnvBuildItem.create(name, value.orElse(null),
                    secret.orElse(null), configmap.orElse(null), field.orElse(null), target, prefix.orElse(null), oldStyle);
            process(kebi);
        } catch (IllegalArgumentException e) {
            errors.add(e.getMessage());
        }
    }

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
            items.values().stream().filter(kebi -> name.equals(kebi.getName())).forEach(existing -> {
                final KubernetesEnvBuildItem.EnvType existingType = existing.getType();
                // if the type doesn't allow multiple definitions, we need to check if we're adding a "compatible" env var
                if (!existingType.allowMultipleDefinitions) {
                    // check if we have a conflict and thus an error
                    if (existingType.mightConflictWith(type)) {
                        // but we first need to check if we're not simply replacing an old style definition by a new one
                        final boolean currentIsNew = existing.isOldStyle() && !item.isOldStyle();
                        final boolean existingIsNew = item.isOldStyle() && !existing.isOldStyle();
                        if ((currentIsNew || existingIsNew) && existingType.equals(type)) {
                            // only keep definition using new style and output warning
                            log.warn("Duplicate definition of '" + name
                                    + "' environment variable. ONLY the quarkus.kubernetes.env prefixed version will be kept: "
                                    + (currentIsNew ? item : existing));
                            if (currentIsNew) {
                                // replace existing, old-style value by current, new-style one
                                wrapper.setShouldAdd(true);
                            }
                        } else {
                            addError(item, existing);
                        }
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
            return items.values();
        }
        throw new IllegalArgumentException(getError());
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
