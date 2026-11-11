package io.quarkus.hibernate.search.standalone.elasticsearch.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.ConstructorMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.ConstructorMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.RootMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorRef;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.index.IndexWrapper;
import io.quarkus.deployment.index.PersistentClassIndex;
import io.quarkus.hibernate.search.backend.elasticsearch.common.deployment.HibernateSearchAccessorDiscovery;

class HibernateSearchAccessorDiscoveryTest {
    @Test
    void discoversBuiltInMappingsWithoutTheirDefinitionsInApplicationIndex() throws IOException {
        assertThat(discover(IndexedType.class, PropertyMappedType.class, ConstructorProjection.class, TypeProjection.class))
                .containsExactlyInAnyOrder(IndexedType.class.getName(), PropertyMappedType.class.getName(),
                        ConstructorProjection.class.getName(), TypeProjection.class.getName());
    }

    @Test
    void discoversCustomAndRepeatedMappings() throws IOException {
        assertThat(discover(CustomPropertyType.class, CustomConstructorType.class, CustomParameterType.class,
                RepeatedType.class, ContainerType.class, UnrelatedContainerType.class, CustomTypeMappedType.class,
                CustomProperty.class, CustomProperties.class, CustomConstructor.class, CustomParameter.class, CustomType.class))
                .containsExactlyInAnyOrder(CustomPropertyType.class.getName(), CustomConstructorType.class.getName(),
                        CustomParameterType.class.getName(), RepeatedType.class.getName(), ContainerType.class.getName(),
                        CustomTypeMappedType.class.getName());
    }

    @Test
    void customMappingAnnotationDeclarationsMustBeIndexed() throws IOException {
        assertThat(discover(CustomPropertyType.class)).isEmpty();
    }

    @Test
    void includesDependencyHierarchiesOnceAndExcludesPlatformTypes() throws IOException {
        assertThat(discover(Child.class, Sibling.class, TypeProjection.class))
                .containsExactlyInAnyOrder(Child.class.getName(), Sibling.class.getName(), Parent.class.getName(),
                        ParentInterface.class.getName(), TypeProjection.class.getName());
    }

    @Test
    void doesNotExpandPropertyValueTypes() throws IOException {
        assertThat(discover(PropertyMappedType.class, UnmappedValue.class))
                .containsExactly(PropertyMappedType.class.getName());
    }

    @Test
    void keepsAccessorDiscoverySeparateFromMappingRoots() throws IOException {
        CombinedIndexBuildItem index = combinedIndex(Index.of(IndexedType.class, PropertyMappedType.class));
        AtomicReference<HibernateSearchStandaloneEnabledBuildItem> enabled = new AtomicReference<>();
        new HibernateSearchStandaloneProcessor().configure(index, enabled::set);
        assertThat(enabled.get().getRootAnnotationMappedClassNames()).containsExactly(IndexedType.class.getName());
        assertThat(HibernateSearchAccessorDiscovery.collectTypes(index))
                .extracting(type -> type.name().toString())
                .containsExactlyInAnyOrder(IndexedType.class.getName(), PropertyMappedType.class.getName());
    }

    @Test
    void preservesExistingRootRegistrations() throws IOException {
        assertThat(discover(CustomRootMappedType.class, CustomRootMapping.class))
                .containsExactly(CustomRootMappedType.class.getName());
    }

    private static List<String> discover(Class<?>... applicationTypes) throws IOException {
        return HibernateSearchAccessorDiscovery.collectTypes(combinedIndex(Index.of(applicationTypes)))
                .stream().map(type -> type.name().toString()).toList();
    }

    private static CombinedIndexBuildItem combinedIndex(Index application) {
        return new CombinedIndexBuildItem(application,
                new IndexWrapper(application, HibernateSearchAccessorDiscoveryTest.class.getClassLoader(),
                        new PersistentClassIndex()));
    }

    @Indexed
    static class IndexedType {
    }

    static class PropertyMappedType {
        @GenericField
        UnmappedValue value;
    }

    static class UnmappedValue {
        String city;
    }

    static class ConstructorProjection {
        @ProjectionConstructor
        ConstructorProjection(String city) {
        }
    }

    @ProjectionConstructor
    record TypeProjection(String city) {
    }

    @Indexed
    static class Child extends Parent implements ParentInterface, Serializable {
    }

    @Indexed
    static class Sibling extends Parent {
    }

    static class Parent {
        String inheritedValue;
    }

    interface ParentInterface {
        default String unannotatedValue() {
            return "value";
        }
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @TypeMapping(processor = @TypeMappingAnnotationProcessorRef(name = "custom"))
    @interface CustomType {
    }

    @CustomType
    static class CustomTypeMappedType {
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @RootMapping
    @interface CustomRootMapping {
    }

    @CustomRootMapping
    static class CustomRootMappedType {
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(name = "custom"))
    @Repeatable(CustomProperties.class)
    @interface CustomProperty {
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface CustomProperties {
        CustomProperty[] value();
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface UnrelatedContainer {
        CustomProperty[] value();
    }

    static class CustomPropertyType {
        @CustomProperty
        String value;
    }

    static class RepeatedType {
        @CustomProperty
        @CustomProperty
        String value;
    }

    static class ContainerType {
        @CustomProperties({ @CustomProperty, @CustomProperty })
        String value;
    }

    static class UnrelatedContainerType {
        @UnrelatedContainer({ @CustomProperty })
        String value;
    }

    @Target(ElementType.CONSTRUCTOR)
    @Retention(RetentionPolicy.RUNTIME)
    @ConstructorMapping(processor = @ConstructorMappingAnnotationProcessorRef(name = "custom"))
    @interface CustomConstructor {
    }

    static class CustomConstructorType {
        @CustomConstructor
        CustomConstructorType(String value) {
        }
    }

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @MethodParameterMapping(processor = @MethodParameterMappingAnnotationProcessorRef(name = "custom"))
    @interface CustomParameter {
    }

    static class CustomParameterType {
        CustomParameterType(@CustomParameter String value) {
        }
    }
}
