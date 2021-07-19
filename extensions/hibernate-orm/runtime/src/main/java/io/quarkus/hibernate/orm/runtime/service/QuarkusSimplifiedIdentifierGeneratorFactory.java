package io.quarkus.hibernate.orm.runtime.service;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.id.Assigned;
import org.hibernate.id.Configurable;
import org.hibernate.id.ForeignGenerator;
import org.hibernate.id.GUIDGenerator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.IncrementGenerator;
import org.hibernate.id.SelectGenerator;
import org.hibernate.id.SequenceHiLoGenerator;
import org.hibernate.id.SequenceIdentityGenerator;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.UUIDHexGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.id.factory.internal.DefaultIdentifierGeneratorFactory;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.Type;

/**
 * Quarkus custom implementation of Hibernate ORM's org.hibernate.id.factory.internal.DefaultIdentifierGeneratorFactory
 * differences with the original:
 * 1# it does not attempt to use a BeanContainer to create instances; Hibernate ORM introduced this feature in HHH-14688
 * 2# No need to handle AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS as Quarkus users shouldn't disable it (on by default)
 */
final class QuarkusSimplifiedIdentifierGeneratorFactory
        implements MutableIdentifierGeneratorFactory, ServiceRegistryAwareService {

    private static final CoreMessageLogger LOG = CoreLogging.messageLogger(DefaultIdentifierGeneratorFactory.class);

    private ServiceRegistry serviceRegistry;
    private Dialect dialect;

    private ConcurrentHashMap<String, Class> generatorStrategyToClassNameMap = new ConcurrentHashMap<String, Class>();

    /**
     * Constructs a new DefaultIdentifierGeneratorFactory.
     */
    public QuarkusSimplifiedIdentifierGeneratorFactory() {
        register("uuid2", UUIDGenerator.class);
        register("guid", GUIDGenerator.class); // can be done with UUIDGenerator + strategy
        register("uuid", UUIDHexGenerator.class); // "deprecated" for new use
        register("uuid.hex", UUIDHexGenerator.class); // uuid.hex is deprecated
        register("assigned", Assigned.class);
        register("identity", IdentityGenerator.class);
        register("select", SelectGenerator.class);
        register("sequence", SequenceStyleGenerator.class);
        register("seqhilo", SequenceHiLoGenerator.class);
        register("increment", IncrementGenerator.class);
        register("foreign", ForeignGenerator.class);
        register("sequence-identity", SequenceIdentityGenerator.class);
        register("enhanced-sequence", SequenceStyleGenerator.class);
        register("enhanced-table", TableGenerator.class);
    }

    public void register(String strategy, Class generatorClass) {
        LOG.debugf("Registering IdentifierGenerator strategy [%s] -> [%s]", strategy, generatorClass.getName());
        final Class previous = generatorStrategyToClassNameMap.put(strategy, generatorClass);
        if (previous != null) {
            LOG.debugf("    - overriding [%s]", previous.getName());
        }
    }

    @Override
    public Dialect getDialect() {
        return dialect;
    }

    @Override
    public void setDialect(Dialect dialect) {
        //This is all commented out in the original code in Hibernate ORM as well
    }

    public IdentifierGenerator createIdentifierGenerator(String strategy, Type type, Properties config) {
        try {
            Class clazz = getIdentifierGeneratorClass(strategy);
            IdentifierGenerator identifierGenerator = (IdentifierGenerator) clazz.getDeclaredConstructor().newInstance();
            if (identifierGenerator instanceof Configurable) {
                ((Configurable) identifierGenerator).configure(type, config, serviceRegistry);
            }
            return identifierGenerator;
        } catch (Exception e) {
            final String entityName = config.getProperty(IdentifierGenerator.ENTITY_NAME);
            throw new MappingException(String.format("Could not instantiate id generator [entity-name=%s]", entityName), e);
        }
    }

    @Override
    public Class getIdentifierGeneratorClass(String strategy) {
        if ("hilo".equals(strategy)) {
            throw new UnsupportedOperationException("Support for 'hilo' generator has been removed");
        }
        String resolvedStrategy = "native".equals(strategy) ? getDialect().getNativeIdentifierGeneratorStrategy() : strategy;

        Class generatorClass = generatorStrategyToClassNameMap.get(resolvedStrategy);
        try {
            if (generatorClass == null) {
                final ClassLoaderService cls = serviceRegistry.getService(ClassLoaderService.class);
                generatorClass = cls.classForName(resolvedStrategy);
            }
        } catch (ClassLoadingException e) {
            throw new MappingException(String.format("Could not interpret id generator strategy [%s]", strategy));
        }
        return generatorClass;
    }

    @Override
    public void injectServices(ServiceRegistryImplementor serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
        this.dialect = serviceRegistry.getService(JdbcEnvironment.class).getDialect();
    }

}
