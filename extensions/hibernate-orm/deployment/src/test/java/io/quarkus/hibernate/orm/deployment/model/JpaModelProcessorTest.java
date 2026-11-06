package io.quarkus.hibernate.orm.deployment.model;

import static io.quarkus.hibernate.orm.deployment.ProcessorTestStubs.emptyHibernateORMConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.deployment.ClassNames;
import io.quarkus.hibernate.orm.deployment.ProcessorTestStubs;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalJpaModelBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalPersistenceUnitBuildItem;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.singlepersistenceunit.DefaultEntity;
import io.quarkus.hibernate.orm.xml.persistence.MyEntity;
import io.quarkus.runtime.util.Reason;

class JpaModelProcessorTest {

    private final JpaModelProcessor processor = new JpaModelProcessor();

    @Test
    void alwaysAddsThePersistenceUnitAnnotation() {
        assertThat(processor.addPersistenceUnitAnnotationToIndex().getClassesToIndex())
                .containsExactly(ClassNames.QUARKUS_PERSISTENCE_UNIT.toString());
    }

    @Test
    void convertsEntityPackageToAnArchiveMarkerPath() {
        var config = ProcessorTestStubs.hibernateORMConfig(Map.of(
                "quarkus.hibernate-orm.packages", "org.acme.defaultpu",
                "quarkus.hibernate-orm.\"inventory\".packages", "org.acme.inventory"));
        var markers = new ArrayList<AdditionalApplicationArchiveMarkerBuildItem>();

        processor.includeArchivesHostingEntityPackagesInIndex(config, markers::add);

        assertThat(markers).extracting(AdditionalApplicationArchiveMarkerBuildItem::getFile)
                .containsExactlyInAnyOrder("org/acme/defaultpu", "org/acme/inventory");
    }

    @Test
    void contributeAdditionalPersistenceUnitModelClassesTest() {
        var additionalPersistenceUnit = AdditionalPersistenceUnitBuildItem.builder("inventory", new Reason("test"))
                .managedClasses(List.of(DefaultEntity.class.getName(), MyEntity.class.getName()))
                .build();

        var additionalModel = new ArrayList<AdditionalJpaModelBuildItem>();

        processor.contributeAdditionalPersistenceUnitModelClasses(
                List.of(additionalPersistenceUnit), additionalModel::add);

        assertThat(additionalModel).extracting(AdditionalJpaModelBuildItem::getClassName)
                .containsExactlyInAnyOrder(DefaultEntity.class.getName(), MyEntity.class.getName());
        assertThat(additionalModel).allSatisfy(item -> assertThat(item.getPersistenceUnits()).containsExactly("inventory"));
    }

    @Test
    void contributeOnePersistenceUnit() {
        var additionalPersistenceUnit = AdditionalPersistenceUnitBuildItem.builder("inventory", new Reason("test"))
                .managedClasses(List.of(DefaultEntity.class.getName(), MyEntity.class.getName()))
                .mappingFiles(List.of("META-INF/default.orm.xml", "META-INF/inventory.orm.xml"))
                .build();
        var persistenceUnitContributions = new ArrayList<JpaModelPersistenceUnitContributionBuildItem>();

        processor.contributeAdditionalPersistenceUnitsToJpaModel(persistenceUnitContributions::add,
                List.of(additionalPersistenceUnit));

        assertThat(persistenceUnitContributions).hasSize(1);
        var contribution = persistenceUnitContributions.getFirst();

        assertThat(contribution.persistenceUnitName).isEqualTo("inventory");
        assertThat(contribution.persistenceUnitRootURL).isNull();
        assertThat(contribution.explicitlyListedClassNames)
                .containsExactlyInAnyOrder(DefaultEntity.class.getName(), MyEntity.class.getName());
        assertThat(contribution.explicitlyListedMappingFiles)
                .containsExactlyInAnyOrder("META-INF/default.orm.xml", "META-INF/inventory.orm.xml");
    }

    @Test
    void createsAndPopulatesDefaultPersistenceUnitModel() throws IOException {
        var model = ProcessorTestStubs.jpaModelWithPackagesAndEntities(
                Set.of(DefaultEntity.class.getPackageName(), MyEntity.class.getPackageName()),
                Set.of(DefaultEntity.class.getName(), MyEntity.class.getName()));

        Index index = Index.of(DefaultEntity.class, MyEntity.class, PersistenceUnit.class, PersistenceUnit.List.class);

        var result = processor.buildJpaModelPerPersistenceUnit(
                emptyHibernateORMConfig(),
                List.of(),
                model,
                new CombinedIndexBuildItem(index, index));

        assertThat(result.getModelPerPersistenceUnit()).containsOnlyKeys(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME);

        var defaultModel = result.getModelPerPersistenceUnit().get(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME);

        assertThat(defaultModel.entityClassNames())
                .containsExactlyInAnyOrder(DefaultEntity.class.getName(), MyEntity.class.getName());
        assertThat(defaultModel.allModelClassNames()).containsExactlyInAnyOrder(
                DefaultEntity.class.getName(),
                MyEntity.class.getName());
        assertThat(defaultModel.modelPackageNames()).containsExactlyInAnyOrder(
                DefaultEntity.class.getPackageName(),
                MyEntity.class.getPackageName());
    }

    @Test
    void createsAndPopulatesDefaultAndNamedPersistenceUnitModels() throws IOException {
        var config = ProcessorTestStubs.hibernateORMConfig(Map.of(
                "quarkus.hibernate-orm.packages", DefaultEntity.class.getPackageName(),
                "quarkus.hibernate-orm.\"inventory\".packages", MyEntity.class.getPackageName()));

        var model = ProcessorTestStubs.jpaModelWithPackagesAndEntities(
                Set.of(DefaultEntity.class.getPackageName(), MyEntity.class.getPackageName()),
                Set.of(DefaultEntity.class.getName(), MyEntity.class.getName()));

        Index index = Index.of(DefaultEntity.class, MyEntity.class, PersistenceUnit.class, PersistenceUnit.List.class);

        var result = processor.buildJpaModelPerPersistenceUnit(
                config,
                List.of(),
                model,
                new CombinedIndexBuildItem(index, index));

        assertThat(result.getPersistenceUnitsConfiguredThroughQuarkusConfiguration())
                .containsExactlyInAnyOrder(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME, "inventory");

        var defaultModel = result.getModelPerPersistenceUnit().get(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME);
        assertThat(defaultModel.entityClassNames()).containsExactly(DefaultEntity.class.getName());
        assertThat(defaultModel.allModelClassNames()).containsExactly(DefaultEntity.class.getName());
        assertThat(defaultModel.modelPackageNames()).containsExactly(DefaultEntity.class.getPackageName());

        var inventoryModel = result.getModelPerPersistenceUnit().get("inventory");
        assertThat(inventoryModel.entityClassNames()).containsExactly(MyEntity.class.getName());
        assertThat(inventoryModel.allModelClassNames()).containsExactly(MyEntity.class.getName());
        assertThat(inventoryModel.modelPackageNames()).containsExactly(MyEntity.class.getPackageName());
    }

    @Test
    void useAdditionalJpaModelToAssignEntityToNamedPersistenceUnit() throws IOException {

        var assignMyEntityToInventoryPU = new AdditionalJpaModelBuildItem(MyEntity.class.getName(), Set.of("inventory"));

        var model = ProcessorTestStubs.jpaModelWithPackagesAndEntities(
                Set.of(DefaultEntity.class.getPackageName(), MyEntity.class.getPackageName()),
                Set.of(DefaultEntity.class.getName(), MyEntity.class.getName()));

        Index index = Index.of(DefaultEntity.class, MyEntity.class, PersistenceUnit.class, PersistenceUnit.List.class);

        var result = processor.buildJpaModelPerPersistenceUnit(
                emptyHibernateORMConfig(),
                List.of(assignMyEntityToInventoryPU),
                model,
                new CombinedIndexBuildItem(index, index));

        assertThat(result.getModelPerPersistenceUnit().get("inventory").entityClassNames())
                .containsExactly(MyEntity.class.getName());

        assertThat(result.getModelPerPersistenceUnit().get(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME)
                .entityClassNames()).containsExactly(DefaultEntity.class.getName());
    }

}
