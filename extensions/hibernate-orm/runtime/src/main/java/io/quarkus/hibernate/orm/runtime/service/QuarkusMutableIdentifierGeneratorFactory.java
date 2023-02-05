package io.quarkus.hibernate.orm.runtime.service;

import java.io.Serializable;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.persistence.GenerationType;

import org.hibernate.dialect.Dialect;
import org.hibernate.generator.Generator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.id.factory.spi.GeneratorDefinitionResolver;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaType;

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
        implements IdentifierGeneratorFactory, Serializable {

    private final QuarkusSimplifiedIdentifierGeneratorFactory original;
    private final ConcurrentHashMap<String, Class<? extends Generator>> typeCache = new ConcurrentHashMap<>();

    public QuarkusMutableIdentifierGeneratorFactory(ServiceRegistry serviceRegistry) {
        this.original = new QuarkusSimplifiedIdentifierGeneratorFactory(serviceRegistry);
    }

    @Override
    public Dialect getDialect() {
        return original.getDialect();
    }

    @Override
    public Generator createIdentifierGenerator(final String strategy, final Type type, final Properties config) {
        final Generator identifierGenerator = original.createIdentifierGenerator(strategy, type, config);
        storeCache(strategy, identifierGenerator.getClass());
        return identifierGenerator;
    }

    private void storeCache(final String strategy, final Class<? extends Generator> generatorClass) {
        if (strategy == null || generatorClass == null)
            return;
        final String className = generatorClass.getName();
        //Store for access both via short and long names:
        typeCache.put(strategy, generatorClass);
        if (!className.equals(strategy)) {
            typeCache.put(className, generatorClass);
        }
    }

    public void register(String strategy, Class generatorClass) {
        storeCache(strategy, generatorClass);
        original.register(strategy, generatorClass);
    }

    @Override
    public Class getIdentifierGeneratorClass(final String strategy) {
        Class<? extends Generator> aClass = typeCache.get(strategy);
        if (aClass != null)
            return aClass;
        aClass = original.getIdentifierGeneratorClass(strategy);
        storeCache(strategy, aClass);
        return aClass;
    }

    @Override
    public IdentifierGenerator createIdentifierGenerator(GenerationType generationType,
            String generatedValueGeneratorName, String generatorName, JavaType<?> javaType, Properties config,
            GeneratorDefinitionResolver definitionResolver) {
        return original.createIdentifierGenerator(generationType, generatedValueGeneratorName, generatorName, javaType, config,
                definitionResolver);
    }

}
