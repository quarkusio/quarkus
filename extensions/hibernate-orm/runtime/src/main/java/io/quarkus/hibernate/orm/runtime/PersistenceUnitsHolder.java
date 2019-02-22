/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.hibernate.orm.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.PersistenceException;

import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import io.quarkus.hibernate.orm.runtime.boot.FastBootMetadataBuilder;
import io.quarkus.hibernate.orm.runtime.boot.LightPersistenceXmlDescriptor;
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
            Scanner scanner) {
        final List<PersistenceUnitDescriptor> units = convertPersistenceUnits(parsedPersistenceXmlDescriptors);
        final Map<String, RecordedState> metadata = constructMetadataAdvance(units, scanner);

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
            final List<PersistenceUnitDescriptor> parsedPersistenceXmlDescriptors, Scanner scanner) {
        Map<String, RecordedState> recordedStates = new HashMap<>();

        for (PersistenceUnitDescriptor unit : parsedPersistenceXmlDescriptors) {
            RecordedState m = createMetadata(unit, scanner);
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

    private static RecordedState createMetadata(PersistenceUnitDescriptor unit, Scanner scanner) {
        FastBootMetadataBuilder fastBootMetadataBuilder = new FastBootMetadataBuilder(unit, scanner);
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
