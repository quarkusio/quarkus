package io.quarkus.hibernate.orm.deployment;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.persistence.spi.PersistenceUnitTransactionType;

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
    public static List<ParsedPersistenceXmlDescriptor> locatePersistenceUnits(
            HibernateOrmConfig.HibernateOrmConfigPersistenceXml config) {
        final QuarkusPersistenceXmlParser parser = new QuarkusPersistenceXmlParser();
        parser.doResolve();

        final List<ParsedPersistenceXmlDescriptor> resolvedPersistenceUnits = parser.getResolvedPersistenceUnits();
        final List<ParsedPersistenceXmlDescriptor> filteredPersistenceUnits = new ArrayList<>();

        resolvedPersistenceUnits.forEach(d -> {
            //if it is not explicitly excluded, it is included
            boolean include = config.excludePersistenceUnit.map(Collection::stream).orElse(Stream.empty())
                    .noneMatch(p -> p.matcher(d.getName()).matches());

            if (!include) {
                //an explicit include overrides an explicit exclude
                include = config.includePersistenceUnit.map(Collection::stream).orElse(Stream.empty())
                        .anyMatch(p -> p.matcher(d.getName()).matches());
            }

            if (include) {
                filteredPersistenceUnits.add(d);
            }

        });

        return filteredPersistenceUnits;
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
