package io.quarkus.hibernate.orm.runtime.service;

import java.io.Serializable;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.dialect.Dialect;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.Type;

/**
 * Wraps the default DefaultIdentifierGeneratorFactory so to make sure we store the Class references
 * of any IdentifierGenerator which is accessed during the build of the Metadata.
 *
 * This is not to register them for reflection access: all reflective instantiation is performed
 * during the build of the Metadata and is therefore safe even in native mode;
 * however we still need the Class instances as some runtime operations will need these, and
 * will look them up by either fully qualified name (and then reflection) or strategy name.
 *
 * Since all IdentifierGenerator types used by a model are accessed during the Metadata creation,
 * just watching for these will provide the full list of Class instances we need to keep.
 */
public final class QuarkusMutableIdentifierGeneratorFactory
        implements MutableIdentifierGeneratorFactory, Serializable, ServiceRegistryAwareService {

    private final QuarkusSimplifiedIdentifierGeneratorFactory original = new QuarkusSimplifiedIdentifierGeneratorFactory();
    private final ConcurrentHashMap<String, Class<? extends IdentifierGenerator>> typeCache = new ConcurrentHashMap<>();

    @Override
    public void register(final String strategy, final Class generatorClass) {
        original.register(strategy, generatorClass);
        storeCache(strategy, generatorClass);
    }

    @Override
    public Dialect getDialect() {
        return original.getDialect();
    }

    @Override
    public void setDialect(final Dialect dialect) {
        //currently a no-op anyway..?
        original.setDialect(dialect);
    }

    @Override
    public IdentifierGenerator createIdentifierGenerator(final String strategy, final Type type, final Properties config) {
        final IdentifierGenerator identifierGenerator = original.createIdentifierGenerator(strategy, type, config);
        storeCache(strategy, identifierGenerator.getClass());
        return identifierGenerator;
    }

    private void storeCache(final String strategy, final Class<? extends IdentifierGenerator> generatorClass) {
        if (strategy == null || generatorClass == null)
            return;
        final String className = generatorClass.getName();
        //Store for access both via short and long names:
        typeCache.put(strategy, generatorClass);
        if (!className.equals(strategy)) {
            typeCache.put(className, generatorClass);
        }
    }

    @Override
    public Class getIdentifierGeneratorClass(final String strategy) {
        Class<? extends IdentifierGenerator> aClass = typeCache.get(strategy);
        if (aClass != null)
            return aClass;
        aClass = original.getIdentifierGeneratorClass(strategy);
        storeCache(strategy, aClass);
        return aClass;
    }

    @Override
    public void injectServices(final ServiceRegistryImplementor serviceRegistry) {
        original.injectServices(serviceRegistry);
    }
}
