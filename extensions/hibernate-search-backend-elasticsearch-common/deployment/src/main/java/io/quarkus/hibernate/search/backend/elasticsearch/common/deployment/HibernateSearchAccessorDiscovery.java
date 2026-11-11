package io.quarkus.hibernate.search.backend.elasticsearch.common.deployment;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.commons.classloading.ClassLoaderHelper;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.util.JandexUtil;

/**
 * Discovers accessor hosts independently of the types used as mapping roots.
 */
public final class HibernateSearchAccessorDiscovery {
    private static final String PROCESSING_PACKAGE = "org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.";
    public static final Set<DotName> MAPPING_META_ANNOTATIONS = Set.of(
            DotName.createSimple(PROCESSING_PACKAGE + "TypeMapping"),
            DotName.createSimple(PROCESSING_PACKAGE + "PropertyMapping"),
            DotName.createSimple(PROCESSING_PACKAGE + "ConstructorMapping"),
            DotName.createSimple(PROCESSING_PACKAGE + "MethodParameterMapping"));

    // Keep this list in sync with Search's mapping annotations;
    // HibernateSearchTypesTest verifies completeness against the actual Search JARs
    //  without indexing those JARs during application builds.
    public static final Set<DotName> BUILT_IN_MAPPING_ANNOTATIONS = Set.of(
            DotName.createSimple("org.hibernate.search.mapper.pojo.bridge.builtin.annotation.AlternativeDiscriminator"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.bridge.builtin.annotation.GeoPointBinding"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Latitude"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Longitude"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.CompositeProjection"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.DistanceProjection"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentReferenceProjection"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.EntityProjection"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.EntityReferenceProjection"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.FieldProjection"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.HighlightProjection"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.IdProjection"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.MarkerBinding"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.NonStandardField"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectProjection"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionBinding"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyBinding"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.ScaledNumberField"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.ScoreProjection"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.TypeBinding"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.VectorField"));
    private static final DotName ROOT_MAPPING = DotName.createSimple(PROCESSING_PACKAGE + "RootMapping");

    private HibernateSearchAccessorDiscovery() {
    }

    public static Collection<ClassInfo> collectTypes(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();
        IndexView definitions = combinedIndex.getComputingIndex();
        Set<DotName> mappingAnnotations = new LinkedHashSet<>(BUILT_IN_MAPPING_ANNOTATIONS);
        for (DotName metaAnnotation : MAPPING_META_ANNOTATIONS) {
            collectMappingAnnotations(index, metaAnnotation, mappingAnnotations);
        }
        // Preserve accessor registration for custom root-only mappings without making accessor discovery
        // depend on the mapping roots collected for Search bootstrap.
        collectMappingAnnotations(index, ROOT_MAPPING, mappingAnnotations);
        Map<DotName, ClassInfo> types = new LinkedHashMap<>();
        for (DotName mappingAnnotation : mappingAnnotations) {
            // Built-in annotation definitions need not be in the application index. Resolve them on demand
            // to let Jandex identify repeatable containers, including custom repeatable annotations.
            for (AnnotationInstance annotation : index.getAnnotationsWithRepeatable(mappingAnnotation, definitions)) {
                ClassInfo type = JandexUtil.getEnclosingClass(annotation);
                if (!type.isAnnotation()) {
                    collectHierarchy(definitions, type.name(), types);
                }
            }
        }
        return List.copyOf(types.values());
    }

    private static void collectMappingAnnotations(IndexView index, DotName metaAnnotation, Set<DotName> annotations) {
        for (AnnotationInstance annotation : index.getAnnotations(metaAnnotation)) {
            annotations.add(annotation.target().asClass().name());
        }
    }

    private static void collectHierarchy(IndexView definitions, DotName name, Map<DotName, ClassInfo> types) {
        if (name == null || DotName.OBJECT_NAME.equals(name) || types.containsKey(name)) {
            return;
        }
        // Skip common JDK hierarchy declarations such as Record, Enum or Serializable.
        // This package check is not exhaustive... but should cover the Search use cases.
        if (ClassLoaderHelper.isInJdkPackage(name.toString())) {
            return;
        }
        ClassInfo type = definitions.getClassByName(name);
        if (type == null) {
            return;
        }
        types.put(name, type);
        collectHierarchy(definitions, type.superName(), types);
        for (DotName interfaceName : type.interfaceNames()) {
            collectHierarchy(definitions, interfaceName, types);
        }
    }
}
