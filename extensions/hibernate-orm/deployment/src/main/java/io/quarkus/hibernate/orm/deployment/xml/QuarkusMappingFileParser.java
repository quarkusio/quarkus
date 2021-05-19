package io.quarkus.hibernate.orm.deployment.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.boot.archive.internal.ArchiveHelper;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.internal.BootstrapServiceRegistryImpl;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.XmlMappingBinderAccess;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.integrator.spi.IntegratorService;

import io.quarkus.hibernate.orm.runtime.boot.xml.RecordableXmlMapping;
import io.quarkus.hibernate.orm.runtime.customized.QuarkusStrategySelectorBuilder;
import io.quarkus.hibernate.orm.runtime.service.FlatClassLoaderService;

/**
 * A parser for orm.xml mapping files.
 */
public final class QuarkusMappingFileParser implements AutoCloseable {

    public static QuarkusMappingFileParser create() {
        BootstrapServiceRegistry serviceRegistry = createEmptyBootstrapServiceRegistry();
        XmlMappingBinderAccess binderAccess = new XmlMappingBinderAccess(serviceRegistry);
        return new QuarkusMappingFileParser(serviceRegistry, binderAccess);
    }

    private final BootstrapServiceRegistry serviceRegistry;
    private final XmlMappingBinderAccess binderAccess;

    public QuarkusMappingFileParser(BootstrapServiceRegistry serviceRegistry, XmlMappingBinderAccess binderAccess) {
        this.serviceRegistry = serviceRegistry;
        this.binderAccess = binderAccess;
    }

    @Override
    public void close() {
        BootstrapServiceRegistryBuilder.destroy(serviceRegistry);
    }

    /**
     * @param persistenceUnitName The name of the persistence unit requesting the mapping file.
     * @param persistenceUnitRootUrl The root URL of the persistence unit requesting the mapping file.
     * @param mappingFilePath The path of the mapping file in the classpath.
     * @return A summary of the parsed mapping file, or {@link Optional#empty()} if it was not found.
     */
    public Optional<RecordableXmlMapping> parse(String persistenceUnitName, URL persistenceUnitRootUrl,
            String mappingFilePath) {
        URL url = locateMappingFile(persistenceUnitName, persistenceUnitRootUrl, mappingFilePath);

        if (url == null) {
            // Ignore and let Hibernate ORM complain about it during bootstrap.
            return Optional.empty();
        }

        try (InputStream stream = url.openStream()) {
            Binding<?> binding = (Binding<?>) binderAccess.bind(stream);
            return Optional.of(RecordableXmlMapping.create(binding));
        } catch (RuntimeException | IOException e) {
            throw new IllegalStateException(
                    "Error reading mapping file '" + mappingFilePath + "' ('" + url + "'): " + e.getMessage(), e);
        }
    }

    private URL locateMappingFile(String persistenceUnitName, URL persistenceUnitRootUrl, String mappingFileName) {
        List<URL> mappingFileURLs = FlatClassLoaderService.INSTANCE.locateResources(mappingFileName);
        if (mappingFileURLs.isEmpty()) {
            return null;
        } else if (mappingFileURLs.size() == 1) {
            return mappingFileURLs.get(0);
        } else { // mappingFileURLs.size() > 1
            // Multiple classpath resources match this name.
            // We need to resolve the ambiguity.
            URL urlInSameMappingFile = null;
            if (persistenceUnitRootUrl != null) {
                for (URL url : mappingFileURLs) {
                    if (!persistenceUnitRootUrl.equals(ArchiveHelper.getJarURLFromURLEntry(url, mappingFileName))) {
                        continue;
                    }
                    if (urlInSameMappingFile == null) {
                        urlInSameMappingFile = url;
                    } else {
                        // Multiple matches in the same JAR...? Can this even happen?
                        urlInSameMappingFile = null;
                        break;
                    }
                }
            }
            if (urlInSameMappingFile == null) {
                throw new IllegalStateException("Persistence unit '" + persistenceUnitName + "' references mapping file '"
                        + mappingFileName + "', but multiple resources with this path exist in the classpath,"
                        + " and it is not possible to resolve the ambiguity."
                        + " URLs of matching resources found in the classpath: " + mappingFileURLs);
            }
            return urlInSameMappingFile;
        }
    }

    private static BootstrapServiceRegistry createEmptyBootstrapServiceRegistry() {
        final ClassLoaderService providedClassLoaderService = FlatClassLoaderService.INSTANCE;
        // N.B. support for integrators removed
        final IntegratorService integratorService = new IntegratorService() {
            @Override
            public Iterable<Integrator> getIntegrators() {
                return Collections.emptyList();
            }
        };
        final QuarkusStrategySelectorBuilder strategySelectorBuilder = new QuarkusStrategySelectorBuilder();
        final StrategySelector strategySelector = strategySelectorBuilder.buildSelector(providedClassLoaderService);
        return new BootstrapServiceRegistryImpl(true, providedClassLoaderService, strategySelector, integratorService);
    }
}
