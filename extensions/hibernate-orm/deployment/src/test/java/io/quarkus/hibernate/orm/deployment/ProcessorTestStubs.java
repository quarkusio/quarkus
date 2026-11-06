package io.quarkus.hibernate.orm.deployment;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import jakarta.persistence.PersistenceUnitTransactionType;

import org.jboss.jandex.DotName;

import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.hibernate.orm.deployment.component.PersistenceUnitDefinitionBuildItem;
import io.quarkus.hibernate.orm.runtime.HibernateOrmPersistenceUnitProviderHelper;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDescriptor;
import io.quarkus.hibernate.orm.runtime.boot.xml.RecordableXmlMapping;
import io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy;
import io.quarkus.hibernate.orm.runtime.recording.RecordedConfig;
import io.quarkus.runtime.configuration.CharsetConverter;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;

public final class ProcessorTestStubs {

    private ProcessorTestStubs() {
    }

    public static HibernateOrmConfig emptyHibernateORMConfig() {
        return hibernateORMConfig(Map.of());
    }

    public static HibernateOrmConfig hibernateORMConfig(Map<String, String> properties) {
        return new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withConverter(Charset.class, 100, new CharsetConverter())
                .withMappingIgnore("quarkus.hibernate-orm.database.charset")
                .withSources(new PropertiesConfigSource(properties, "test", 100))
                .withMapping(HibernateOrmConfig.class)
                .build()
                .getConfigMapping(HibernateOrmConfig.class);
    }

    public static JpaModelBuildItem emptyJpaModel() {
        return jpaModel(Set.of(), Set.of(), Set.of(), Set.of());
    }

    public static JpaModelBuildItem jpaModelWithManagedClasses(String... managedClasses) {
        return jpaModel(Set.of(), Set.of(), Set.of(managedClasses), Set.of());
    }

    public static JpaModelBuildItem jpaModelWithPotentialCdiBeans(DotName... potentialCdiBeans) {
        return jpaModel(Set.of(), Set.of(), Set.of(), Set.of(potentialCdiBeans));
    }

    public static JpaModelBuildItem jpaModelWithPackagesAndEntities(Set<String> packages, Set<String> entities) {
        return jpaModel(packages, entities, entities, Set.of());
    }

    public static JpaModelBuildItem jpaModelForScanner(Set<String> packages, Set<String> entities,
            Set<String> managedClasses) {
        return jpaModel(packages, entities, managedClasses, Set.of());
    }

    public static PersistenceUnitDefinitionBuildItem persistenceUnitDefinition(String name,
            ProgrammingParadigm paradigm, String dataSource) {
        return new PersistenceUnitDefinitionBuildItem(name, paradigm, List.of(new Reason("test")), null,
                Optional.of(dataSource), Optional.empty());
    }

    private static JpaModelBuildItem jpaModel(Set<String> packages, Set<String> entities, Set<String> managedClasses,
            Set<DotName> potentialCdiBeans) {
        return new JpaModelBuildItem(packages, entities, managedClasses, potentialCdiBeans, Map.of());
    }

    public static PersistenceUnitDescriptorBuildItem persistenceUnitDescriptor() {
        return persistenceUnitDescriptor(List.of());
    }

    public static PersistenceUnitDescriptorBuildItem persistenceUnitDescriptor(String name,
            MultiTenancyStrategy multiTenancyStrategy) {
        return persistenceUnitDescriptor(name, List.of(), multiTenancyStrategy);
    }

    public static PersistenceUnitDescriptorBuildItem persistenceUnitDescriptorWithXmlMapping(
            RecordableXmlMapping xmlMapping) {
        return persistenceUnitDescriptor(List.of(xmlMapping));
    }

    public static PersistenceUnitDescriptorBuildItem persistenceUnitDescriptorWithoutXmlMappings() {
        return persistenceUnitDescriptor(List.of());
    }

    private static PersistenceUnitDescriptorBuildItem persistenceUnitDescriptor(List<RecordableXmlMapping> xmlMappings) {
        return persistenceUnitDescriptor("test", xmlMappings, MultiTenancyStrategy.NONE);
    }

    private static PersistenceUnitDescriptorBuildItem persistenceUnitDescriptor(String name,
            List<RecordableXmlMapping> xmlMappings, MultiTenancyStrategy multiTenancyStrategy) {
        var descriptor = new QuarkusPersistenceUnitDescriptor(
                name,
                new HibernateOrmPersistenceUnitProviderHelper(),
                PersistenceUnitTransactionType.JTA,
                List.of(),
                List.of(),
                new Properties(),
                false);

        var recordedConfig = new RecordedConfig(
                Optional.empty(),
                Optional.of(DatabaseKind.H2),
                Optional.of(DatabaseKind.H2),
                Optional.empty(),
                false,
                Optional.empty(),
                Set.of(),
                multiTenancyStrategy,
                null, Map.of());

        return new PersistenceUnitDescriptorBuildItem(descriptor,
                recordedConfig,
                xmlMappings,
                false,
                false);
    }
}
