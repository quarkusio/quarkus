package io.quarkus.hibernate.panache.deployment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import io.quarkus.hibernate.orm.deployment.JpaModelPersistenceUnitMappingBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;

//FIXME: duplicate with ORM and probably HR
public final class EntityToPersistenceUnitUtil {

    @FunctionalInterface
    public interface TriConsumer {
        void consume(String entity, String persistenceUnit, String reactivePersistenceUnit);
    }

    private EntityToPersistenceUnitUtil() {
    }

    /**
     * Given the candidate entities, return a map with the persistence unit that single contains them
     * or throw an exception if any of the candidates is part of more than one persistence unit
     */
    public static void determineEntityPersistenceUnits(
            Optional<JpaModelPersistenceUnitMappingBuildItem> jpaModelPersistenceUnitMapping,
            List<PersistenceUnitDescriptorBuildItem> descriptors,
            Set<String> candidates, String source,
            TriConsumer consumer) {
        if (jpaModelPersistenceUnitMapping.isEmpty()) {
            return;
        }
        Map<String, String> result = new HashMap<>();
        Map<String, Set<String>> collectedEntityToPersistenceUnits = jpaModelPersistenceUnitMapping.get()
                .getEntityToPersistenceUnits();

        Map<String, List<PersistenceUnitDescriptorBuildItem>> descriptorsMap = new HashMap<>();
        for (PersistenceUnitDescriptorBuildItem descriptor : descriptors) {
            descriptorsMap.computeIfAbsent(descriptor.getPersistenceUnitName(), k -> new ArrayList<>());
            descriptorsMap.get(descriptor.getPersistenceUnitName()).add(descriptor);
        }

        Map<String, Set<String>> violatingEntities = new TreeMap<>();

        for (Map.Entry<String, Set<String>> entry : collectedEntityToPersistenceUnits.entrySet()) {
            String entityName = entry.getKey();
            Set<String> selectedPersistenceUnits = entry.getValue();
            boolean isCandidate = candidates.contains(entityName);

            if (!isCandidate) {
                continue;
            }

            String persistenceUnit = null;
            String reactivePersistenceUnit = null;
            if (selectedPersistenceUnits.size() > 0) {
                boolean error = false;
                // collect at most one of blocking/reactive PU
                for (String selectedPersistenceUnit : selectedPersistenceUnits) {
                    List<PersistenceUnitDescriptorBuildItem> matchingDescriptors = descriptorsMap.get(selectedPersistenceUnit);
                    if (matchingDescriptors == null) {
                        throw new IllegalStateException(String.format("Cannot find descriptor unit named %s for entity %s",
                                selectedPersistenceUnit, entityName));
                    }
                    for (PersistenceUnitDescriptorBuildItem descriptor : matchingDescriptors) {
                        if (descriptor.isReactive()) {
                            if (reactivePersistenceUnit != null) {
                                error = true;
                                break;
                            } else {
                                reactivePersistenceUnit = selectedPersistenceUnit;
                            }
                        } else {
                            if (persistenceUnit != null) {
                                error = true;
                                break;
                            } else {
                                persistenceUnit = selectedPersistenceUnit;
                            }
                        }
                    }
                }
                if (error) {
                    violatingEntities.put(entityName, selectedPersistenceUnits);
                } else if (persistenceUnit != null || reactivePersistenceUnit != null) {
                    // ignore the entity if it belongs to no PU, though I doubt that happens
                    consumer.consume(entityName, persistenceUnit, reactivePersistenceUnit);
                }
            }
        }

        if (violatingEntities.size() > 0) {
            StringBuilder message = new StringBuilder(
                    String.format("%s entities do not support being attached to several persistence units:\n", source));
            for (Map.Entry<String, Set<String>> violatingEntityEntry : violatingEntities
                    .entrySet()) {
                message.append("\t- ").append(violatingEntityEntry.getKey()).append(" is attached to: ")
                        .append(String.join(",", violatingEntityEntry.getValue()));
                throw new IllegalStateException(message.toString());
            }
        }
    }
}
