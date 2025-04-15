package io.quarkus.hibernate.orm.runtime;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.integrator.spi.Integrator;

import io.quarkus.hibernate.orm.runtime.boot.FastBootMetadataBuilder;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDefinition;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDescriptor;
import io.quarkus.hibernate.orm.runtime.proxies.PreGeneratedProxies;
import io.quarkus.hibernate.orm.runtime.recording.RecordedState;

public final class PersistenceUnitsHolder {

    private static final String NO_NAME_TOKEN = "__no_name";

    // Populated by Quarkus's runtime phase from Quarkus deployment info
    private static volatile PersistenceUnits persistenceUnits;

    /**
     * Initialize JPA for use in Quarkus. In a native image. This must be called
     * from within a static init method.
     *
     * The scanner may be null to use the default scanner, or a custom scanner can be
     * used to stop Hibernate scanning. It is expected that the scanner will be
     * provided by Quarkus via its hold of Jandex info.
     *
     * @param puDefinitions
     * @param scanner
     */
    static void initializeJpa(List<QuarkusPersistenceUnitDefinition> puDefinitions,
            Scanner scanner, Collection<Class<? extends Integrator>> additionalIntegrators,
            PreGeneratedProxies preGeneratedProxies) {
        final List<QuarkusPersistenceUnitDescriptor> units = convertPersistenceUnits(puDefinitions);
        final Map<RecordedStateKey, RecordedState> metadata = constructMetadataAdvance(puDefinitions, scanner,
                additionalIntegrators,
                preGeneratedProxies);

        persistenceUnits = new PersistenceUnits(units, metadata);
    }

    public static List<QuarkusPersistenceUnitDescriptor> getPersistenceUnitDescriptors() {
        checkJPAInitialization();
        return persistenceUnits.units;
    }

    public static RecordedState popRecordedState(String persistenceUnitName, boolean isReactive) {
        checkJPAInitialization();
        RecordedStateKey key = new RecordedStateKey(persistenceUnitName, isReactive);
        if (persistenceUnitName == null) {
            key = new RecordedStateKey(NO_NAME_TOKEN, isReactive);
        }
        return persistenceUnits.recordedStates.remove(key);
    }

    private static List<QuarkusPersistenceUnitDescriptor> convertPersistenceUnits(
            final List<QuarkusPersistenceUnitDefinition> parsedPersistenceXmlDescriptors) {
        return parsedPersistenceXmlDescriptors.stream().map(QuarkusPersistenceUnitDefinition::getPersistenceUnitDescriptor)
                .collect(Collectors.toList());
    }

    record RecordedStateKey(String name, boolean isReactive) {
    }

    private static Map<RecordedStateKey, RecordedState> constructMetadataAdvance(
            final List<QuarkusPersistenceUnitDefinition> parsedPersistenceXmlDescriptors, Scanner scanner,
            Collection<Class<? extends Integrator>> additionalIntegrators,
            PreGeneratedProxies proxyClassDefinitions) {
        Map<RecordedStateKey, RecordedState> recordedStates = new HashMap<>();

        for (QuarkusPersistenceUnitDefinition unit : parsedPersistenceXmlDescriptors) {
            RecordedState m = createMetadata(unit, scanner, additionalIntegrators, proxyClassDefinitions);
            String name = unitName(unit);
            RecordedStateKey key = new RecordedStateKey(name, unit.isReactive());
            Object previous = recordedStates.put(key, m);
            if (previous != null) {
                throw new IllegalStateException("Duplicate persistence unit name: " + unit.getName());
            }
        }

        return recordedStates;
    }

    private static void checkJPAInitialization() {
        if (persistenceUnits == null) {
            throw new RuntimeException("JPA not initialized yet by Quarkus: this is likely a bug.");
        }
    }

    private static String unitName(QuarkusPersistenceUnitDefinition unit) {
        String name = unit.getName();
        if (name == null) {
            return NO_NAME_TOKEN;
        }
        return name;
    }

    public static RecordedState createMetadata(QuarkusPersistenceUnitDefinition unit, Scanner scanner,
            Collection<Class<? extends Integrator>> additionalIntegrators, PreGeneratedProxies proxyDefinitions) {
        FastBootMetadataBuilder fastBootMetadataBuilder = new FastBootMetadataBuilder(unit, scanner, additionalIntegrators,
                proxyDefinitions);
        return fastBootMetadataBuilder.build();
    }

    private static class PersistenceUnits {

        private final List<QuarkusPersistenceUnitDescriptor> units;

        private final Map<RecordedStateKey, RecordedState> recordedStates;

        public PersistenceUnits(final List<QuarkusPersistenceUnitDescriptor> units,
                final Map<RecordedStateKey, RecordedState> recordedStates) {
            this.units = units;
            this.recordedStates = recordedStates;
        }
    }

}
