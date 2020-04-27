package io.quarkus.kubernetes.deployment;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
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
    private final Map<String, Set<KubernetesEnvBuildItem>> errors = new HashMap<>();

    /**
     * Processes the specified {@link KubernetesEnvBuildItem} to check whether it's valid with respect to the set of already
     * known configuration, accumulating errors or outputting warnings if needed.
     *
     * @param item the {@link KubernetesEnvBuildItem} to validate
     */
    void process(KubernetesEnvBuildItem item) {
        final String name = item.getName();
        final KEBIWrapper wrapper = new KEBIWrapper(item);
        // check if we already have defined a build item with the same name
        if (knownNames.contains(name)) {
            final KubernetesEnvBuildItem.EnvType type = item.getType();
            // as the item might not get added, reset the wrapper's state
            wrapper.setNeedingAdding(false);
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
                                    + (currentIsNew ? describe(item) : describe(existing)));
                            if (currentIsNew) {
                                // replace existing, old-style value by current, new-style one
                                wrapper.setNeedingAdding(true);
                            }
                        } else {
                            addError(item, existing);
                        }
                    } else {
                        // we're not dealing with a potentially conflicting var so add the new item
                        wrapper.setNeedingAdding(true);
                    }
                } else {
                    if (existingType.mightConflictWith(type)) {
                        log.warn("Ignoring duplicate definition of " + describe(item));
                    }
                    wrapper.setNeedingAdding(true);

                }
            });
        }
        if (wrapper.isNeedingAdding()) {
            items.put(ItemKey.keyFor(item), item);
        }
        knownNames.add(name);
    }

    private void addError(KubernetesEnvBuildItem item, KubernetesEnvBuildItem existing) {
        final Set<KubernetesEnvBuildItem> inError = errors.computeIfAbsent(item.getName(), k -> new LinkedHashSet<>());
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
        if (errors.isEmpty()) {
            return items.values();
        }
        throw new IllegalArgumentException(getError());
    }

    private String getError() {
        String error = "Found conflicts in environment variable definitions:\n";
        error += errors.entrySet().stream()
                .map(e -> {
                    final String conflicting = e.getValue().stream()
                            .map(this::describe)
                            .collect(Collectors.joining(" redefined as "));
                    return String.format("\t\t- '%s': first defined as %s", e.getKey(), conflicting);
                })
                .collect(Collectors.joining("\n"));
        return error;
    }

    private String describe(KubernetesEnvBuildItem kebi) {
        return String.format("'%s' env var with value '%s'", kebi.getType().name(), kebi.getValue());
    }

    private static final class KEBIWrapper {
        private final KubernetesEnvBuildItem item;
        private boolean needingAdding;

        KEBIWrapper(KubernetesEnvBuildItem item) {
            this.item = item;
            needingAdding = true;
        }

        public boolean isNeedingAdding() {
            return needingAdding;
        }

        public void setNeedingAdding(boolean needingAdding) {
            this.needingAdding = needingAdding;
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
