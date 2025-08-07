package io.quarkus.hibernate.orm.runtime;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
     * <p>
     * The scanner may be null to use the default scanner, or a custom scanner can be
     * used to stop Hibernate scanning. It is expected that the scanner will be
     * provided by Quarkus via its hold of Jandex info.
     */
    static void initializeJpa(
            List<QuarkusPersistenceUnitDefinition> puDefinitions,
            Scanner scanner,
            Collection<Class<? extends Integrator>> additionalIntegrators,
            PreGeneratedProxies preGeneratedProxies) {
        persistenceUnits = constructMetadataAdvance(puDefinitions, scanner, additionalIntegrators, preGeneratedProxies);
    }

    public static Map<PersistenceUnitKey, QuarkusPersistenceUnitDescriptor> getPersistenceUnits() {
        checkJPAInitialization();
        return persistenceUnits.units;
    }

    public static Collection<QuarkusPersistenceUnitDescriptor> getPersistenceUnitDescriptors() {
        return getPersistenceUnits().values();
    }

    public static QuarkusPersistenceUnitDescriptor getPersistenceUnitDescriptor(String persistenceUnitName,
            boolean isReactive) {
        checkJPAInitialization();
        return persistenceUnits.units.get(new PersistenceUnitKey(unitName(persistenceUnitName), isReactive));
    }

    public static RecordedState popRecordedState(String persistenceUnitName, boolean isReactive) {
        checkJPAInitialization();
        PersistenceUnitKey key = new PersistenceUnitKey(unitName(persistenceUnitName), isReactive);
        return persistenceUnits.recordedStates.remove(key);
    }

    private static PersistenceUnits constructMetadataAdvance(
            final List<QuarkusPersistenceUnitDefinition> parsedPersistenceXmlDescriptors, Scanner scanner,
            Collection<Class<? extends Integrator>> additionalIntegrators,
            PreGeneratedProxies proxyClassDefinitions) {
        int size = parsedPersistenceXmlDescriptors.size();
        Map<PersistenceUnitKey, QuarkusPersistenceUnitDescriptor> units = new HashMap<>(size);
        Map<PersistenceUnitKey, RecordedState> recordedStates = new HashMap<>(size);

        for (QuarkusPersistenceUnitDefinition unit : parsedPersistenceXmlDescriptors) {
            PersistenceUnitKey key = new PersistenceUnitKey(unitName(unit.getName()), unit.isReactive());
            Object previous = units.put(key, unit.getPersistenceUnitDescriptor());
            if (previous != null) {
                throw new IllegalStateException(String.format(
                        Locale.ROOT,
                        "Duplicate persistence unit name: %s",
                        unit.getName() + (unit.isReactive() ? " (reactive)" : "")));
            }

            RecordedState metadata = createMetadata(unit, scanner, additionalIntegrators, proxyClassDefinitions);
            recordedStates.put(key, metadata);
        }

        return new PersistenceUnits(units, recordedStates);
    }

    private static void checkJPAInitialization() {
        if (persistenceUnits == null) {
            throw new RuntimeException("JPA not initialized yet by Quarkus: this is likely a bug.");
        }
    }

    private static String unitName(String name) {
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

        private final Map<PersistenceUnitKey, QuarkusPersistenceUnitDescriptor> units;

        private final Map<PersistenceUnitKey, RecordedState> recordedStates;

        public PersistenceUnits(final Map<PersistenceUnitKey, QuarkusPersistenceUnitDescriptor> units,
                final Map<PersistenceUnitKey, RecordedState> recordedStates) {
            this.units = Collections.unmodifiableMap(units);
            this.recordedStates = recordedStates;
        }
    }

}
