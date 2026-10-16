package io.quarkus.hibernate.orm.deployment.bytecode;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.hibernate.orm.deployment.ClassNames;
import io.quarkus.hibernate.orm.deployment.ProcessorTestStubs;
import io.quarkus.hibernate.orm.runtime.boot.xml.RecordableXmlMapping;

class BytecodeProcessorXmlMappingAnnotationProxyTest {

    private final BytecodeProcessor processor = new BytecodeProcessor();

    @Test
    void registersMappingAnnotationsForReflection() {
        var xmlMapping = new RecordableXmlMapping(
                new JaxbEntityMappingsImpl(),
                null,
                SourceType.RESOURCE,
                "META-INF/orm.xml");

        var descriptor = ProcessorTestStubs.persistenceUnitDescriptorWithXmlMapping(xmlMapping);

        List<ReflectiveClassBuildItem> reflection = new ArrayList<>();
        List<NativeImageProxyDefinitionBuildItem> proxies = new ArrayList<>();

        var expectedAnnotations = new HashSet<String>();
        expectedAnnotations.addAll(ClassNames.JPA_MAPPING_ANNOTATIONS.stream().map(Object::toString).toList());
        expectedAnnotations.addAll(ClassNames.HIBERNATE_MAPPING_ANNOTATIONS.stream().map(Object::toString).toList());

        processor.preGenAnnotationProxies(List.of(descriptor), reflection::add, proxies::add);

        assertThat(reflection).singleElement().satisfies(item -> {
            assertThat(item.getClassNames()).containsExactlyInAnyOrderElementsOf(expectedAnnotations);
            assertThat(item.isMethods()).isTrue();
            assertThat(item.isFields()).isTrue();
            assertThat(item.getReason()).isEqualTo(ClassNames.HIBERNATE_ORM_PROCESSOR.toString());
        });

        assertThat(proxies).hasSize(expectedAnnotations.size());
        assertThat(proxies).flatExtracting(NativeImageProxyDefinitionBuildItem::getClasses)
                .containsExactlyInAnyOrderElementsOf(expectedAnnotations);
    }

    @Test
    void registersNothingWhenNoPersistenceUnitContainsXmlMappings() {
        var descriptor = ProcessorTestStubs.persistenceUnitDescriptorWithoutXmlMappings();
        List<ReflectiveClassBuildItem> reflection = new ArrayList<>();
        List<NativeImageProxyDefinitionBuildItem> proxies = new ArrayList<>();

        processor.preGenAnnotationProxies(List.of(descriptor), reflection::add, proxies::add);

        assertThat(reflection).isEmpty();
        assertThat(proxies).isEmpty();
    }
}
