package io.quarkus.hibernate.orm.deployment.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
     * @param mappingFileName The name of the mapping file.
     * @return A summary of the parsed mapping file, or {@link Optional#empty()} if it was not found.
     */
    public Optional<RecordableXmlMapping> parse(String mappingFileName) {
        URL url = locateMappingFile(mappingFileName);

        if (url == null) {
            // Ignore and let Hibernate ORM complain about it during bootstrap.
            return Optional.empty();
        }

        try (InputStream stream = url.openStream()) {
            Binding<?> binding = (Binding<?>) binderAccess.bind(stream);
            return Optional.of(RecordableXmlMapping.create(binding));
        } catch (RuntimeException | IOException e) {
            throw new IllegalStateException(
                    "Error reading mapping file '" + mappingFileName + "' ('" + url + "'): " + e.getMessage(), e);
        }
    }

    private URL locateMappingFile(String mappingFileName) {
        List<URL> mappingFileURLs = FlatClassLoaderService.INSTANCE.locateResources(mappingFileName);
        if (mappingFileURLs.isEmpty()) {
            return null;
        }
        if (mappingFileURLs.size() > 1) {
            throw new IllegalStateException("Founds multiple occurrences of mapping file '" + mappingFileName
                    + "' in the classpath: " + mappingFileURLs);
        }
        return mappingFileURLs.get(0);
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
