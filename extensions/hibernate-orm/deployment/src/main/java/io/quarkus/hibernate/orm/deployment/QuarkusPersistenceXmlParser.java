package io.quarkus.hibernate.orm.deployment;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;

import io.quarkus.hibernate.orm.runtime.service.FlatClassLoaderService;

/**
 * Similar to Hibernate ORM's PersistenceXmlParser to adapt it to
 * specific needs in Quarkus.
 */
final class QuarkusPersistenceXmlParser extends PersistenceXmlParser {

    /**
     * Similar to {@link PersistenceXmlParser#locatePersistenceUnits(Map)}
     * except it doesn't need a properties map, and avoids complaining if no resources are found.
     *
     * @return the list of ParsedPersistenceXmlDescriptor(s), after discovery and parsing.
     */
    public static List<ParsedPersistenceXmlDescriptor> locatePersistenceUnits() {
        final QuarkusPersistenceXmlParser parser = new QuarkusPersistenceXmlParser();
        parser.doResolve();
        return parser.getResolvedPersistenceUnits();
    }

    private QuarkusPersistenceXmlParser() {
        //N.B. RESOURCE_LOCAL is matching the default in Hibernate ORM; we use the same here as persistence.xml is treated as "legacy"
        // yet this is not the default that Quarkus will use when booting via `application.properties`.
        super(FlatClassLoaderService.INSTANCE, PersistenceUnitTransactionType.RESOURCE_LOCAL);
    }

    private void doResolve() {
        final List<URL> xmlUrls = FlatClassLoaderService.INSTANCE.locateResources("META-INF/persistence.xml");
        for (URL xmlUrl : xmlUrls) {
            parsePersistenceXml(xmlUrl, Collections.emptyMap());
        }
    }

}
