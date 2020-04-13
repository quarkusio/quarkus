package io.quarkus.hibernate.orm.runtime;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.PersistenceException;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.service.spi.ServiceContributor;

import io.quarkus.hibernate.orm.runtime.boot.FastBootMetadataBuilder;
import io.quarkus.hibernate.orm.runtime.boot.LightPersistenceXmlDescriptor;
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
     * In general the <code>parsedPersistenceXmlDescriptors</code> will be provided
     * by calling {@link #loadOriginalXMLParsedDescriptors()} In Quarkus this is
     * done in Quarkus's JPA ResourceProcessor.
     *
     * The scanner may be null to use the default scanner, or a custom scanner can be
     * used to stop Hibernate scanning. It is expected that the scanner will be
     * provided by Quarkus via its hold of Jandex info.
     *
     * @param parsedPersistenceXmlDescriptors
     * @param scanner
     */
    static void initializeJpa(List<ParsedPersistenceXmlDescriptor> parsedPersistenceXmlDescriptors,
            Scanner scanner, Collection<Class<? extends Integrator>> additionalIntegrators,
            Collection<Class<? extends ServiceContributor>> additionalServiceContributors,
            PreGeneratedProxies preGeneratedProxies, MultiTenancyStrategy strategy) {

        final List<PersistenceUnitDescriptor> units = convertPersistenceUnits(parsedPersistenceXmlDescriptors);
        final Map<String, RecordedState> metadata = constructMetadataAdvance(units, scanner, additionalIntegrators,
                preGeneratedProxies, strategy);

        persistenceUnits = new PersistenceUnits(units, metadata);
    }

    static List<PersistenceUnitDescriptor> getPersistenceUnitDescriptors() {
        checkJPAInitialization();
        return persistenceUnits.units;
    }

    static RecordedState getRecordedState(String persistenceUnitName) {
        checkJPAInitialization();
        Object key = persistenceUnitName;
        if (persistenceUnitName == null) {
            key = NO_NAME_TOKEN;
        }
        return persistenceUnits.recordedStates.get(key);
    }

    private static List<PersistenceUnitDescriptor> convertPersistenceUnits(
            final List<ParsedPersistenceXmlDescriptor> parsedPersistenceXmlDescriptors) {
        try {
            return parsedPersistenceXmlDescriptors.stream().map(LightPersistenceXmlDescriptor::new)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new PersistenceException("Unable to locate persistence units", e);
        }
    }

    private static Map<String, RecordedState> constructMetadataAdvance(
            final List<PersistenceUnitDescriptor> parsedPersistenceXmlDescriptors, Scanner scanner,
            Collection<Class<? extends Integrator>> additionalIntegrators,
            PreGeneratedProxies proxyClassDefinitions, MultiTenancyStrategy strategy) {
        Map<String, RecordedState> recordedStates = new HashMap<>();

        for (PersistenceUnitDescriptor unit : parsedPersistenceXmlDescriptors) {
            RecordedState m = createMetadata(unit, scanner, additionalIntegrators, proxyClassDefinitions, strategy);
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

    private static String unitName(PersistenceUnitDescriptor unit) {
        String name = unit.getName();
        if (name == null) {
            return NO_NAME_TOKEN;
        }
        return name;
    }

    public static RecordedState createMetadata(PersistenceUnitDescriptor unit, Scanner scanner,
            Collection<Class<? extends Integrator>> additionalIntegrators, PreGeneratedProxies proxyDefinitions, MultiTenancyStrategy strategy) {
        FastBootMetadataBuilder fastBootMetadataBuilder = new FastBootMetadataBuilder(unit, scanner, additionalIntegrators,
                proxyDefinitions, strategy);
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
