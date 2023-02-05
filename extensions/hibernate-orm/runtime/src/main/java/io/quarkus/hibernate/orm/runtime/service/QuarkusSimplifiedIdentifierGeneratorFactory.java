package io.quarkus.hibernate.orm.runtime.service;

import static org.hibernate.cfg.AvailableSettings.IDENTIFIER_GENERATOR_STRATEGY_PROVIDER;
import static org.hibernate.id.factory.IdGenFactoryLogging.ID_GEN_FAC_LOGGER;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.persistence.GenerationType;

import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.generator.Generator;
import org.hibernate.id.Assigned;
import org.hibernate.id.Configurable;
import org.hibernate.id.ForeignGenerator;
import org.hibernate.id.GUIDGenerator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.IncrementGenerator;
import org.hibernate.id.SelectGenerator;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.UUIDHexGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.id.factory.internal.AutoGenerationTypeStrategy;
import org.hibernate.id.factory.internal.IdentityGenerationTypeStrategy;
import org.hibernate.id.factory.internal.SequenceGenerationTypeStrategy;
import org.hibernate.id.factory.internal.TableGenerationTypeStrategy;
import org.hibernate.id.factory.internal.UUIDGenerationTypeStrategy;
import org.hibernate.id.factory.spi.GenerationTypeStrategy;
import org.hibernate.id.factory.spi.GenerationTypeStrategyRegistration;
import org.hibernate.id.factory.spi.GeneratorDefinitionResolver;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.jpa.spi.IdentifierGeneratorStrategyProvider;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Quarkus custom implementation of Hibernate ORM's org.hibernate.id.factory.internal.StandardIdentifierGeneratorFactory
 * differences with the original:
 * 1# it does not attempt to use a BeanContainer to create instances; Hibernate ORM introduced this feature in HHH-14688
 * 2# The register method is made public
 * //TODO refactor ORM upstream so to allow code reuse
 */
final class QuarkusSimplifiedIdentifierGeneratorFactory
        implements IdentifierGeneratorFactory {

    private final ServiceRegistry serviceRegistry;
    private final ConcurrentHashMap<GenerationType, GenerationTypeStrategy> generatorTypeStrategyMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Class<? extends Generator>> legacyGeneratorClassNameMap = new ConcurrentHashMap<>();

    private Dialect dialect;

    public QuarkusSimplifiedIdentifierGeneratorFactory(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
        registerJpaGenerators();
        logOverrides();
        registerPredefinedGenerators();
        registerUsingLegacyContributor();
    }

    //Same-as-upstream
    private void registerJpaGenerators() {
        generatorTypeStrategyMap.put(GenerationType.AUTO, AutoGenerationTypeStrategy.INSTANCE);
        generatorTypeStrategyMap.put(GenerationType.SEQUENCE, SequenceGenerationTypeStrategy.INSTANCE);
        generatorTypeStrategyMap.put(GenerationType.TABLE, TableGenerationTypeStrategy.INSTANCE);
        generatorTypeStrategyMap.put(GenerationType.IDENTITY, IdentityGenerationTypeStrategy.INSTANCE);
        generatorTypeStrategyMap.put(GenerationType.UUID, UUIDGenerationTypeStrategy.INSTANCE);
    }

    private void logOverrides() {
        serviceRegistry.getService(ClassLoaderService.class)
                .loadJavaServices(GenerationTypeStrategyRegistration.class)
                .forEach((registration) -> registration.registerStrategies(
                        (generationType, generationTypeStrategy) -> {
                            final GenerationTypeStrategy previous = generatorTypeStrategyMap.put(generationType,
                                    generationTypeStrategy);
                            if (previous != null) {
                                ID_GEN_FAC_LOGGER.debugf(
                                        "GenerationTypeStrategyRegistration [%s] overrode previous registration for GenerationType#%s : %s",
                                        registration,
                                        generationType.name(),
                                        previous);
                            }
                        },
                        serviceRegistry));
    }

    //Same-as-upstream
    private void registerPredefinedGenerators() {
        register("uuid2", UUIDGenerator.class);
        // can be done with UuidGenerator + strategy
        register("guid", GUIDGenerator.class);
        register("uuid", UUIDHexGenerator.class); // "deprecated" for new use
        register("uuid.hex", UUIDHexGenerator.class); // uuid.hex is deprecated
        register("assigned", Assigned.class);
        register("identity", IdentityGenerator.class);
        register("select", SelectGenerator.class);
        register("sequence", SequenceStyleGenerator.class);
        register("increment", IncrementGenerator.class);
        register("foreign", ForeignGenerator.class);
        register("enhanced-sequence", SequenceStyleGenerator.class);
        register("enhanced-table", TableGenerator.class);
    }

    //Same-as-upstream
    private void registerUsingLegacyContributor() {
        final ConfigurationService configService = serviceRegistry.getService(ConfigurationService.class);
        final Object providerSetting = configService.getSettings().get(IDENTIFIER_GENERATOR_STRATEGY_PROVIDER);
        if (providerSetting != null) {
            DeprecationLogger.DEPRECATION_LOGGER.deprecatedSetting2(
                    IDENTIFIER_GENERATOR_STRATEGY_PROVIDER,
                    "supply a org.hibernate.id.factory.spi.GenerationTypeStrategyRegistration Java service");
            final IdentifierGeneratorStrategyProvider idGeneratorStrategyProvider = serviceRegistry
                    .getService(StrategySelector.class)
                    .resolveStrategy(IdentifierGeneratorStrategyProvider.class, providerSetting);
            for (Map.Entry<String, Class<?>> entry : idGeneratorStrategyProvider.getStrategies().entrySet()) {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                Class<? extends IdentifierGenerator> generatorClass = (Class) entry.getValue();
                register(entry.getKey(), generatorClass);
            }
        }
    }

    //Same-as-upstream - but made public
    public void register(String strategy, Class generatorClass) {
        ID_GEN_FAC_LOGGER.debugf("Registering IdentifierGenerator strategy [%s] -> [%s]", strategy, generatorClass.getName());
        final Class<?> previous = legacyGeneratorClassNameMap.put(strategy, generatorClass);
        if (previous != null && ID_GEN_FAC_LOGGER.isDebugEnabled()) {
            ID_GEN_FAC_LOGGER.debugf("    - overriding [%s]", previous.getName());
        }
    }

    @Override //Same-as-upstream
    public IdentifierGenerator createIdentifierGenerator(
            GenerationType generationType,
            String generatedValueGeneratorName,
            String generatorName,
            JavaType<?> javaType,
            Properties config,
            GeneratorDefinitionResolver definitionResolver) {
        final GenerationTypeStrategy strategy = generatorTypeStrategyMap.get(generationType);
        if (strategy != null) {
            return strategy.createIdentifierGenerator(
                    generationType,
                    generatorName,
                    javaType,
                    config,
                    definitionResolver,
                    serviceRegistry);
        }
        throw new UnsupportedOperationException("No GenerationTypeStrategy specified");
    }

    @Override //Same-as-upstream
    public Dialect getDialect() {
        if (dialect == null) {
            dialect = serviceRegistry.getService(JdbcEnvironment.class).getDialect();
        }
        return dialect;
    }

    //Different than upstream: ignore all complexity related to it having generators
    //managed by a bean container.
    @Override
    public Generator createIdentifierGenerator(String strategy, Type type, Properties parameters) {
        final Class<? extends Generator> clazz = getIdentifierGeneratorClass(strategy);
        try {
            final Generator identifierGenerator = clazz.getConstructor().newInstance();
            if (identifierGenerator instanceof Configurable) {
                ((Configurable) identifierGenerator).configure(type, parameters, serviceRegistry);
            }
            return identifierGenerator;
        } catch (Exception e) {
            final String entityName = parameters.getProperty(IdentifierGenerator.ENTITY_NAME);
            throw new MappingException("Could not instantiate id generator [entity-name=" + entityName + "]");
        }
    }

    @Override //Same-as-upstream
    public Class getIdentifierGeneratorClass(String strategy) {
        if ("hilo".equals(strategy)) {
            throw new UnsupportedOperationException("Support for 'hilo' generator has been removed");
        }
        String resolvedStrategy = "native".equals(strategy) ? getDialect().getNativeIdentifierGeneratorStrategy() : strategy;

        Class generatorClass = legacyGeneratorClassNameMap.get(resolvedStrategy);
        try {
            if (generatorClass == null) {
                final ClassLoaderService cls = serviceRegistry.getService(ClassLoaderService.class);
                generatorClass = cls.classForName(resolvedStrategy);
            }
        } catch (ClassLoadingException e) {
            throw new MappingException("Could not interpret id generator strategy [" + strategy + "]");
        }
        return generatorClass;
    }

}
