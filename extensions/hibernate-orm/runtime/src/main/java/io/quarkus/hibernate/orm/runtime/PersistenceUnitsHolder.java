package io.quarkus.hibernate.orm.runtime;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import io.quarkus.hibernate.orm.runtime.boot.FastBootMetadataBuilder;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDefinition;
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
        final List<PersistenceUnitDescriptor> units = convertPersistenceUnits(puDefinitions);
        final Map<String, RecordedState> metadata = constructMetadataAdvance(puDefinitions, scanner, additionalIntegrators,
                preGeneratedProxies);

        persistenceUnits = new PersistenceUnits(units, metadata);
    }

    public static List<PersistenceUnitDescriptor> getPersistenceUnitDescriptors() {
        checkJPAInitialization();
        return persistenceUnits.units;
    }

    public static RecordedState popRecordedState(String persistenceUnitName) {
        checkJPAInitialization();
        Object key = persistenceUnitName;
        if (persistenceUnitName == null) {
            key = NO_NAME_TOKEN;
        }
        return persistenceUnits.recordedStates.remove(key);
    }

    private static List<PersistenceUnitDescriptor> convertPersistenceUnits(
            final List<QuarkusPersistenceUnitDefinition> parsedPersistenceXmlDescriptors) {
        return parsedPersistenceXmlDescriptors.stream().map(QuarkusPersistenceUnitDefinition::getActualHibernateDescriptor)
                .collect(Collectors.toList());
    }

    private static Map<String, RecordedState> constructMetadataAdvance(
            final List<QuarkusPersistenceUnitDefinition> parsedPersistenceXmlDescriptors, Scanner scanner,
            Collection<Class<? extends Integrator>> additionalIntegrators,
            PreGeneratedProxies proxyClassDefinitions) {
        Map<String, RecordedState> recordedStates = new HashMap<>();

        for (QuarkusPersistenceUnitDefinition unit : parsedPersistenceXmlDescriptors) {
            RecordedState m = createMetadata(unit, scanner, additionalIntegrators, proxyClassDefinitions);
            Object previous = recordedStates.put(unitName(unit), m);
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

        private final List<PersistenceUnitDescriptor> units;

        private final Map<String, RecordedState> recordedStates;

        public PersistenceUnits(final List<PersistenceUnitDescriptor> units, final Map<String, RecordedState> recordedStates) {
            this.units = units;
            this.recordedStates = recordedStates;
        }
    }

}
