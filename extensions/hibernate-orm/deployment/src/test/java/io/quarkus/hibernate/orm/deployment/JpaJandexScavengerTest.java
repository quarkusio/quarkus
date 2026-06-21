package io.quarkus.hibernate.orm.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

import org.hibernate.boot.models.internal.OrmAnnotationHelper;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.hibernate.orm.packages.ParentEntity;
import io.quarkus.hibernate.orm.xml.hbm.NonAnnotatedComponent;
import io.quarkus.hibernate.orm.xml.hbm.NonAnnotatedComponentUsingEntity;
import io.quarkus.hibernate.orm.xml.hbm.NonAnnotatedEntity;
import io.quarkus.hibernate.orm.xml.orm.AnnotatedEntity;
import io.quarkus.hibernate.orm.xml.orm.OtherNonAnnotatedEntity;
import io.quarkus.runtime.configuration.ConfigurationException;

public class JpaJandexScavengerTest {

    @MappedSuperclass
    static class BaseMappedSuperclass {
        @Id
        long id;
    }

    @Entity
    static class SimpleEntity extends BaseMappedSuperclass {
        String name;
    }

    @Embeddable
    static class SimpleEmbeddable {
        String street;
    }

    enum MyStatus {
        ACTIVE,
        INACTIVE
    }

    @Entity
    static class EntityWithEmbedded {
        @Id
        long id;
        @Embedded
        SimpleEmbeddable address;
    }

    @Entity
    static class EntityWithEmbeddedId {
        @EmbeddedId
        SimpleEmbeddable compositeId;
    }

    @Entity
    static class EntityWithElementCollection {
        @Id
        long id;
        @ElementCollection
        List<SimpleEmbeddable> addresses;
    }

    @Entity
    static class EntityWithEnum {
        @Id
        long id;
        MyStatus status;
    }

    static class MyIdClass {
        long part1;
        long part2;
    }

    @Entity
    @IdClass(MyIdClass.class)
    static class EntityWithIdClass {
        @Id
        long part1;
        @Id
        long part2;
    }

    static class MyListener {
        @PrePersist
        void onPrePersist(Object entity) {
        }
    }

    @Entity
    @EntityListeners(MyListener.class)
    static class EntityWithListener {
        @Id
        long id;
    }

    @Entity
    static class ChildEntity extends SimpleEntity {
        String extra;
    }

    // Class used as @Embedded but missing @Embeddable annotation
    static class NotAnEmbeddable {
        String value;
    }

    @Entity
    static class EntityWithMissingEmbeddable {
        @Id
        long id;
        @Embedded
        NotAnEmbeddable broken;
    }

    @Entity
    static class SerializableEntity implements Serializable {
        @Id
        long id;
        String name;
    }

    // Non-annotated classes — only defined as JPA types via orm.xml
    static class XmlOnlyEntity {
        long id;
        String name;
    }

    static class XmlOnlyMappedSuperclass {
        String superField;
    }

    static class XmlOnlyEmbeddable {
        String value;
    }

    // Converter and related classes for testing @Converter(autoApply = true)
    static class MyData {
        String value;

        MyData() {
        }

        MyData(String value) {
            this.value = value;
        }
    }

    @Converter(autoApply = true)
    static class MyDataConverter implements AttributeConverter<MyData, String> {
        @Override
        public String convertToDatabaseColumn(MyData attribute) {
            return attribute == null ? null : attribute.value;
        }

        @Override
        public MyData convertToEntityAttribute(String dbData) {
            return dbData == null ? null : new MyData(dbData);
        }
    }

    @Entity
    static class EntityWithConvertedField {
        @Id
        long id;
        MyData myData;
    }

    @Test
    void entityAndHierarchyDiscovery() throws Exception {
        Index index = buildIndex(SimpleEntity.class, BaseMappedSuperclass.class);

        runScavenger(index)
                .entityClassesHave(SimpleEntity.class)
                .entityClassesDoNotHave(BaseMappedSuperclass.class)
                .managedClassesHave(SimpleEntity.class, BaseMappedSuperclass.class);
    }

    @Test
    void embeddedFieldDiscovery() throws Exception {
        Index index = buildIndex(EntityWithEmbedded.class, SimpleEmbeddable.class);

        runScavenger(index)
                .entityClassesHave(EntityWithEmbedded.class)
                .entityClassesDoNotHave(SimpleEmbeddable.class)
                .managedClassesHave(SimpleEmbeddable.class);
    }

    @Test
    void embeddedIdOnMethodDiscovery() throws Exception {
        Index index = buildIndex(EntityWithEmbeddedId.class, SimpleEmbeddable.class);

        runScavenger(index)
                .entityClassesHave(EntityWithEmbeddedId.class)
                .entityClassesDoNotHave(SimpleEmbeddable.class)
                .managedClassesHave(SimpleEmbeddable.class);
    }

    @Test
    void elementCollectionDiscovery() throws Exception {
        Index index = buildIndex(EntityWithElementCollection.class, SimpleEmbeddable.class);

        runScavenger(index)
                .entityClassesHave(EntityWithElementCollection.class)
                .entityClassesDoNotHave(SimpleEmbeddable.class)
                .managedClassesHave(SimpleEmbeddable.class);
    }

    @Test
    void enumFieldDetection() throws Exception {
        Index index = buildIndex(EntityWithEnum.class, MyStatus.class);

        runScavenger(index)
                .entityClassesHave(EntityWithEnum.class)
                .entityClassesDoNotHave(MyStatus.class)
                .managedClassesDoNotHave(MyStatus.class);
    }

    @Test
    void entityInheritanceDiscovery() throws Exception {
        Index index = buildIndex(ChildEntity.class, SimpleEntity.class, BaseMappedSuperclass.class);

        runScavenger(index)
                .entityClassesHave(SimpleEntity.class, ChildEntity.class)
                .entityClassesDoNotHave(BaseMappedSuperclass.class)
                .managedClassesHave(SimpleEntity.class, ChildEntity.class, BaseMappedSuperclass.class);
    }

    @Test
    void entityListenerDiscovery() throws Exception {
        Index index = buildIndex(EntityWithListener.class, MyListener.class);

        runScavenger(index)
                .entityClassesHave(EntityWithListener.class)
                .potentialCdiBeanClassNamesHave(MyListener.class);
    }

    @Test
    void idClassDiscovery() throws Exception {
        Index index = buildIndex(EntityWithIdClass.class, MyIdClass.class);

        runScavenger(index)
                .entityClassesHave(EntityWithIdClass.class)
                .entityClassesDoNotHave(MyIdClass.class);
    }

    @Test
    void idClassIsManagedClass() throws Exception {
        Index index = buildIndex(EntityWithIdClass.class, MyIdClass.class);

        runScavenger(index)
                .managedClassesHave(MyIdClass.class);
    }

    @Test
    void xmlOnlyEntityDiscovery() throws Exception {
        Index index = buildIndex(XmlOnlyEntity.class, XmlOnlyMappedSuperclass.class, XmlOnlyEmbeddable.class);

        runScavenger(index, Collections.emptySet(), "META-INF/orm-test-discovery.xml")
                .entityClassesHave(XmlOnlyEntity.class)
                .entityClassesDoNotHave(XmlOnlyMappedSuperclass.class, XmlOnlyEmbeddable.class)
                .managedClassesHave(XmlOnlyEntity.class, XmlOnlyMappedSuperclass.class, XmlOnlyEmbeddable.class);
    }

    @Test
    void parseHbmTest() throws Exception {
        Index index = buildIndex(NonAnnotatedComponentUsingEntity.class, NonAnnotatedComponent.class);

        runScavenger(index, Collections.emptySet(), "META-INF/hbm-component.xml")
                .entityClassesHave(NonAnnotatedComponentUsingEntity.class, NonAnnotatedComponent.class);
    }

    @Test
    void unindexedSuperclassThrowsException() throws Exception {
        Index index = buildIndex(SimpleEntity.class);

        assertThatThrownBy(() -> runScavenger(index))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage(String.format(Locale.ROOT, """
                        Unable to properly register the hierarchy of the following JPA classes \
                        as they are not in the Jandex index:
                        \t- %s
                        Consider adding them to the index either by creating a Jandex index \
                        for your dependency via the Maven plugin, an empty META-INF/beans.xml \
                        or quarkus.index-dependency properties.""", BaseMappedSuperclass.class.getName()));
    }

    @Test
    void unindexedSuperclassIgnoredWhenInIgnorableSet() throws Exception {
        Index index = buildIndex(SimpleEntity.class);
        Set<String> ignorable = Set.of(BaseMappedSuperclass.class.getName());

        runScavenger(index, ignorable)
                .entityClassesHave(SimpleEntity.class);
    }

    @Test
    void embeddedFieldWithoutEmbeddableAnnotationThrowsException() throws Exception {
        Index index = buildIndex(EntityWithMissingEmbeddable.class, NotAnEmbeddable.class);

        assertThatThrownBy(() -> runScavenger(index))
                .hasMessageContaining("must be annotated with @Embeddable");
    }

    @Test
    void hbmSimpleEntityDiscovery() throws Exception {
        Index index = buildIndex(NonAnnotatedEntity.class);

        runScavenger(index, Collections.emptySet(), "META-INF/hbm-simple.xml")
                .entityClassesHave(NonAnnotatedEntity.class)
                .managedClassesHave(NonAnnotatedEntity.class);
    }

    @Test
    void hbmFilterDefEntityDiscovery() throws Exception {
        Index index = buildIndex(NonAnnotatedEntity.class);

        runScavenger(index, Collections.emptySet(), "META-INF/hbm-filterdef.xml")
                .entityClassesHave(NonAnnotatedEntity.class)
                .managedClassesHave(NonAnnotatedEntity.class);
    }

    @Test
    void ormSimpleEntityDiscovery() throws Exception {
        Index index = buildIndex(io.quarkus.hibernate.orm.xml.orm.NonAnnotatedEntity.class);

        runScavenger(index, Collections.emptySet(), "META-INF/orm-simple.xml")
                .entityClassesHave(io.quarkus.hibernate.orm.xml.orm.NonAnnotatedEntity.class)
                .managedClassesHave(io.quarkus.hibernate.orm.xml.orm.NonAnnotatedEntity.class);
    }

    @Test
    void ormPackageEntityDiscovery() throws Exception {
        Index index = buildIndex(io.quarkus.hibernate.orm.xml.orm.NonAnnotatedEntity.class,
                OtherNonAnnotatedEntity.class);

        runScavenger(index, Collections.emptySet(), "META-INF/orm-package.xml")
                .entityClassesHave(io.quarkus.hibernate.orm.xml.orm.NonAnnotatedEntity.class,
                        OtherNonAnnotatedEntity.class)
                .managedClassesHave(io.quarkus.hibernate.orm.xml.orm.NonAnnotatedEntity.class,
                        OtherNonAnnotatedEntity.class);
    }

    @Test
    void converterAutoApplyDiscovery() throws Exception {
        Index index = buildIndex(EntityWithConvertedField.class, MyDataConverter.class, MyData.class);

        runScavenger(index)
                .entityClassesHave(EntityWithConvertedField.class)
                .potentialCdiBeanClassNamesHave(MyDataConverter.class);
    }

    @Test
    void converterInAllModelClasses() throws Exception {
        Index index = buildIndex(EntityWithConvertedField.class, MyDataConverter.class, MyData.class);

        runScavenger(index)
                .allModelClassNamesHave(MyDataConverter.class);
    }

    @Test
    void packageLevelAnnotationDiscovery() throws Exception {
        Index index = buildIndex(ParentEntity.class,
                Class.forName("io.quarkus.hibernate.orm.packages.package-info"));

        runScavenger(index)
                .entityClassesHave(ParentEntity.class)
                .allModelPackageNamesHave("io.quarkus.hibernate.orm.packages");
    }

    @Test
    void hbmSimpleParseXmlPu() throws Exception {
        Index index = buildIndex(NonAnnotatedEntity.class);

        runScavenger(index, Collections.emptySet(), "META-INF/hbm-simple.xml")
                .xmlMappingsHavePU("default");
    }

    @Test
    void hbmFilterDefParseXmlPu() throws Exception {
        Index index = buildIndex(NonAnnotatedEntity.class);

        runScavenger(index, Collections.emptySet(), "META-INF/hbm-filterdef.xml")
                .xmlMappingsHavePU("default");
    }

    @Test
    void hbmComponentParseXmlPu() throws Exception {
        Index index = buildIndex(NonAnnotatedComponentUsingEntity.class, NonAnnotatedComponent.class);

        runScavenger(index, Collections.emptySet(), "META-INF/hbm-component.xml")
                .xmlMappingsHavePU("default");
    }

    @Test
    void ormOverrideParseXmlPu() throws Exception {
        Index index = buildIndex(AnnotatedEntity.class);

        runScavenger(index, Collections.emptySet(), "META-INF/orm-override.xml")
                .xmlMappingsHavePU("default");
    }

    @Test
    void ormSimpleParseXmlPu() throws Exception {
        Index index = buildIndex(io.quarkus.hibernate.orm.xml.orm.NonAnnotatedEntity.class);

        runScavenger(index, Collections.emptySet(), "META-INF/orm-simple.xml")
                .xmlMappingsHavePU("default");
    }

    @Test
    void ormPackageParseXmlPu() throws Exception {
        Index index = buildIndex(io.quarkus.hibernate.orm.xml.orm.NonAnnotatedEntity.class,
                OtherNonAnnotatedEntity.class);

        runScavenger(index, Collections.emptySet(), "META-INF/orm-package.xml")
                .xmlMappingsHavePU("default");
    }

    @Test
    void xmlMappingInvalidSilentlyAccepted() throws Exception {
        // JpaJandexScavenger only uses XML to extract class names and never validates
        // attribute members, so an orm.xml referencing non-existent properties
        // (e.g. propertythatdoesnotexist1) is silently accepted.
        Index index = buildIndex(io.quarkus.hibernate.orm.xml.orm.NonAnnotatedEntity.class);

        runScavenger(index, Collections.emptySet(), "orm-invalid-nonannotated.xml");
    }

    @Test
    void enumFieldInEnumTypes() throws Exception {
        Index index = buildIndex(EntityWithEnum.class, MyStatus.class);

        runScavenger(index)
                .enumTypesHave(MyStatus.class);
    }

    @Test
    void javaPackageInterfaceInJavaTypes() throws Exception {
        Index index = buildIndex(SerializableEntity.class);

        runScavenger(index)
                .javaTypesHave(Serializable.class);
    }

    @Test
    void noFileMappingShouldNotDiscoverEntitiesOrEnums() throws Exception {
        Index index = buildIndex(AnnotatedEntity.class);

        runScavenger(index, Collections.emptySet(), "no-file")
                .entityClassesHave(AnnotatedEntity.class);
    }

    @Test
    void noFileMappingShouldNotDiscoverEntities() throws Exception {
        Index index = buildIndex(NonAnnotatedEntity.class);

        runScavenger(index, Collections.emptySet(), "no-file")
                .entityClassesDoNotHave(NonAnnotatedEntity.class)
                .managedClassesDoNotHave(NonAnnotatedEntity.class);
    }

    @Test
    void xmlMappingFilesRegisteredForHotDeployment() throws Exception {
        Index index = buildIndex(io.quarkus.hibernate.orm.xml.orm.NonAnnotatedEntity.class);

        runScavenger(index, Collections.emptySet(), "META-INF/orm-simple.xml")
                .hotDeploymentWatchedFilesHave("META-INF/orm-simple.xml");
    }

    @Test
    void implicitOrmXmlDiscovery() throws Exception {
        Index index = buildIndex(SimpleEntity.class, BaseMappedSuperclass.class);

        runScavenger(index)
                .xmlMappingsHavePU("default");
    }

    private ScavengerResult runScavenger(Index index) throws Exception {
        return runScavenger(index, Collections.emptySet());
    }

    private ScavengerResult runScavenger(Index index, Set<String> ignorableNonIndexedClasses,
            String... explicitlyListedMappingFiles) throws Exception {
        List<JpaModelPersistenceUnitContributionBuildItem> contributions = new ArrayList<>();
        if (explicitlyListedMappingFiles.length > 0) {
            contributions.add(new JpaModelPersistenceUnitContributionBuildItem(
                    "default", null, Collections.emptySet(), Arrays.asList(explicitlyListedMappingFiles)));
        } else {
            contributions.add(new JpaModelPersistenceUnitContributionBuildItem(
                    "default", null, Collections.emptySet(), Collections.emptyList()));
        }

        List<HotDeploymentWatchedFileBuildItem> watchedFiles = new ArrayList<>();
        JpaJandexScavenger scavenger = new JpaJandexScavenger(
                new ArrayList<ReflectiveClassBuildItem>()::add,
                watchedFiles::add,
                contributions,
                index,
                ignorableNonIndexedClasses);

        JpaJandexScavenger.Collector collector = scavenger.collectModel();
        JpaModelBuildItem result = scavenger.buildModelFromCollector(collector);
        return new ScavengerResult(result, collector.enumTypes, collector.javaTypes,
                watchedFiles.stream().map(HotDeploymentWatchedFileBuildItem::getLocation).toList());
    }

    private Index buildIndex(Class<?>... classes) throws Exception {
        Indexer indexer = new Indexer();
        OrmAnnotationHelper.forEachOrmAnnotation(descriptor -> {
            try {
                indexer.indexClass(descriptor.getAnnotationType());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        indexer.indexClass(Deprecated.class);
        for (Class<?> clazz : classes) {
            indexer.indexClass(clazz);
        }
        return indexer.complete();
    }

    private record ScavengerResult(JpaModelBuildItem result, Set<String> enumTypes, Set<String> javaTypes,
            List<String> hotDeploymentWatchedFiles) {

        private static List<String> toNames(Class<?>... classes) {
            return Arrays.stream(classes).map(Class::getName).toList();
        }

        private ScavengerResult assertHas(Set<String> actual, Class<?>... classes) {
            assertThat(actual).containsAll(toNames(classes));
            return this;
        }

        private ScavengerResult assertDoesNotHave(Set<String> actual, Class<?>... classes) {
            assertThat(actual).doesNotContainAnyElementsOf(toNames(classes));
            return this;
        }

        ScavengerResult entityClassesHave(Class<?>... classes) {
            return assertHas(result.getEntityClassNames(), classes);
        }

        ScavengerResult entityClassesDoNotHave(Class<?>... classes) {
            return assertDoesNotHave(result.getEntityClassNames(), classes);
        }

        ScavengerResult managedClassesHave(Class<?>... classes) {
            return assertHas(result.getManagedClassNames(), classes);
        }

        ScavengerResult managedClassesDoNotHave(Class<?>... classes) {
            return assertDoesNotHave(result.getManagedClassNames(), classes);
        }

        ScavengerResult potentialCdiBeanClassNamesHave(Class<?>... classes) {
            Set<String> beanClassNames = result.getPotentialCdiBeanClassNames().stream()
                    .map(dn -> dn.toString())
                    .collect(Collectors.toSet());
            assertThat(beanClassNames).containsAll(toNames(classes));
            return this;
        }

        ScavengerResult allModelPackageNamesHave(String... packageNames) {
            assertThat(result.getAllModelPackageNames()).containsAll(Arrays.asList(packageNames));
            return this;
        }

        ScavengerResult allModelClassNamesHave(Class<?>... classes) {
            assertThat(result.getAllModelClassNames()).containsAll(toNames(classes));
            return this;
        }

        ScavengerResult xmlMappingsHavePU(String puName) {
            assertThat(result.getXmlMappings(puName)).isNotEmpty();
            return this;
        }

        ScavengerResult enumTypesHave(Class<?>... classes) {
            assertThat(enumTypes).containsAll(toNames(classes));
            return this;
        }

        ScavengerResult javaTypesHave(Class<?>... classes) {
            assertThat(javaTypes).containsAll(toNames(classes));
            return this;
        }

        ScavengerResult hotDeploymentWatchedFilesHave(String... fileNames) {
            assertThat(hotDeploymentWatchedFiles).containsAll(Arrays.asList(fileNames));
            return this;
        }
    }
}
