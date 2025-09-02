package io.quarkus.hibernate.orm.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmDiscriminatorSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmEntityBaseDefinition;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmJoinedSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmUnionSubclassEntityType;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntity;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerContainerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityOrMappedSuperclass;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManagedType;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Declaration;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.builder.BuildException;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.hibernate.orm.deployment.xml.QuarkusMappingFileParser;
import io.quarkus.hibernate.orm.runtime.boot.xml.RecordableXmlMapping;
import io.quarkus.runtime.configuration.ConfigurationException;

/**
 * Scan the Jandex index to find JPA entities (and embeddables supporting entity models).
 * <p>
 * The output is then both consumed as plain list to use as a filter for which classes
 * need to be enhanced, collect them for storage in the JPADeploymentTemplate and registered
 * for reflective access.
 * TODO some of these are going to be redundant?
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public final class JpaJandexScavenger {

    public static final List<DotName> EMBEDDED_ANNOTATIONS = Arrays.asList(ClassNames.EMBEDDED_ID, ClassNames.EMBEDDED);

    private static final String XML_MAPPING_DEFAULT_ORM_XML = "META-INF/orm.xml";
    private static final String XML_MAPPING_NO_FILE = "no-file";

    private final BuildProducer<ReflectiveClassBuildItem> reflectiveClass;
    private final BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles;
    private final List<JpaModelPersistenceUnitContributionBuildItem> persistenceUnitContributions;
    private final IndexView index;
    private final Set<String> ignorableNonIndexedClasses;

    JpaJandexScavenger(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles,
            List<JpaModelPersistenceUnitContributionBuildItem> persistenceUnitContributions,
            IndexView index,
            Set<String> ignorableNonIndexedClasses) {
        this.reflectiveClass = reflectiveClass;
        this.hotDeploymentWatchedFiles = hotDeploymentWatchedFiles;
        this.persistenceUnitContributions = persistenceUnitContributions;
        this.index = index;
        this.ignorableNonIndexedClasses = ignorableNonIndexedClasses;
    }

    public JpaModelBuildItem discoverModelAndRegisterForReflection() throws BuildException {
        Collector collector = new Collector();

        for (DotName packageAnnotation : ClassNames.PACKAGE_ANNOTATIONS) {
            enlistJPAModelAnnotatedPackages(collector, packageAnnotation);
        }
        enlistJPAModelClasses(collector, ClassNames.JPA_ENTITY);
        enlistJPAModelClasses(collector, ClassNames.EMBEDDABLE);
        enlistJPAModelClasses(collector, ClassNames.MAPPED_SUPERCLASS);
        enlistJPAModelIdClasses(collector, ClassNames.ID_CLASS);
        enlistEmbeddedsAndElementCollections(collector);

        enlistPotentialCdiBeanClasses(collector, ClassNames.CONVERTER);
        for (DotName annotation : ClassNames.JPA_LISTENER_ANNOTATIONS) {
            enlistPotentialCdiBeanClasses(collector, annotation);
        }

        enlistPotentialClassReferences(collector, ClassNames.GENERIC_GENERATOR, "type", "strategy");
        enlistPotentialClassReferences(collector, ClassNames.ID_GENERATOR_TYPE, "value");
        enlistPotentialClassReferences(collector, ClassNames.VALUE_GENERATION_TYPE, "generatedBy");

        for (JpaModelPersistenceUnitContributionBuildItem persistenceUnitContribution : persistenceUnitContributions) {
            enlistExplicitMappings(collector, persistenceUnitContribution);
        }

        Set<String> managedClassNames = new HashSet<>(collector.entityTypes);
        managedClassNames.addAll(collector.modelTypes);
        for (String className : managedClassNames) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(className).methods().fields().build());
            // Register static metamodel classes as well, so that their `class_` attribute can be populated.
            // See org.hibernate.metamodel.internal.MetadataContext.populateStaticMetamodel
            // Note: registering classes that do not exist is not a problem -- and is necessary if the application
            //       tries to access these classes via reflection anyway (which it will, through Hibernate ORM).
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(className + "_").fields().build());
        }

        if (!collector.enumTypes.isEmpty()) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder("java.lang.Enum").methods().build());
            for (String className : collector.enumTypes) {
                reflectiveClass.produce(ReflectiveClassBuildItem.builder(className).methods().build());
            }
        }

        // for the java types we collected (usually from java.time but it could be from other types),
        // we just register them for reflection
        for (String javaType : collector.javaTypes) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(javaType).methods().build());
        }

        if (!collector.unindexedClasses.isEmpty()) {
            Set<String> unIgnorableIndexedClasses = collector.unindexedClasses.stream().map(DotName::toString)
                    .collect(Collectors.toSet());
            unIgnorableIndexedClasses.removeAll(ignorableNonIndexedClasses);

            if (!unIgnorableIndexedClasses.isEmpty()) {
                final String unindexedClassesErrorMessage = unIgnorableIndexedClasses.stream().map(d -> "\t- " + d + "\n")
                        .collect(Collectors.joining());
                throw new ConfigurationException(
                        "Unable to properly register the hierarchy of the following JPA classes as they are not in the Jandex index:\n"
                                + unindexedClassesErrorMessage
                                + "Consider adding them to the index either by creating a Jandex index " +
                                "for your dependency via the Maven plugin, an empty META-INF/beans.xml or quarkus.index-dependency properties.");
            }
        }

        return new JpaModelBuildItem(collector.packages, collector.entityTypes, managedClassNames,
                collector.potentialCdiBeanTypes, collector.xmlMappingsByPU);
    }

    private void enlistExplicitMappings(Collector collector,
            JpaModelPersistenceUnitContributionBuildItem persistenceUnitContribution) {
        // Classes explicitly mentioned in persistence.xml
        for (String className : persistenceUnitContribution.explicitlyListedClassNames) {
            enlistExplicitClass(collector, className);
        }

        // Classes explicitly mentioned in a mapping file
        Set<String> mappingFileNames = new LinkedHashSet<>(persistenceUnitContribution.explicitlyListedMappingFiles);
        if (!mappingFileNames.remove(XML_MAPPING_NO_FILE)) {
            mappingFileNames.add(XML_MAPPING_DEFAULT_ORM_XML);
        }
        try (QuarkusMappingFileParser parser = QuarkusMappingFileParser.create()) {
            for (String mappingFileName : mappingFileNames) {
                hotDeploymentWatchedFiles.produce(new HotDeploymentWatchedFileBuildItem(mappingFileName));

                Optional<RecordableXmlMapping> mappingOptional = parser.parse(persistenceUnitContribution.persistenceUnitName,
                        persistenceUnitContribution.persistenceUnitRootURL, mappingFileName);
                if (!mappingOptional.isPresent()) {
                    if (persistenceUnitContribution.explicitlyListedMappingFiles.contains(mappingFileName)) {
                        // Trigger an exception for files that are explicitly mentioned and could not be found
                        // DEFAULT_ORM_XML in particular may be mentioned only implicitly,
                        // in which case it's fine if we cannot find it.
                        throw new IllegalStateException("Cannot find ORM mapping file '" + mappingFileName
                                + "' in the classpath");
                    }
                    continue;
                }
                RecordableXmlMapping mapping = mappingOptional.get();
                if (mapping.getOrmXmlRoot() != null) {
                    enlistOrmXmlMapping(collector, mapping.getOrmXmlRoot());
                }
                if (mapping.getHbmXmlRoot() != null) {
                    enlistHbmXmlMapping(collector, mapping.getHbmXmlRoot());
                }
                collector.xmlMappingsByPU
                        .computeIfAbsent(persistenceUnitContribution.persistenceUnitName, ignored -> new ArrayList<>())
                        .add(mapping);
            }
        }
    }

    private void enlistOrmXmlMapping(Collector collector, JaxbEntityMappingsImpl mapping) {
        String packageName = mapping.getPackage();
        String packagePrefix = packageName == null ? "" : packageName + ".";

        var metadata = mapping.getPersistenceUnitMetadata();
        var defaults = metadata == null ? null : metadata.getPersistenceUnitDefaults();
        if (defaults != null) {
            enlistOrmXmlMappingListeners(collector, packagePrefix, defaults.getEntityListenerContainer());
        }

        for (var entity : mapping.getEntities()) {
            enlistOrmXmlMappingManagedClass(collector, packagePrefix, entity, "entity");
        }
        for (var mappedSuperclass : mapping.getMappedSuperclasses()) {
            enlistOrmXmlMappingManagedClass(collector, packagePrefix, mappedSuperclass, "mapped-superclass");
        }
        for (var embeddable : mapping.getEmbeddables()) {
            String name = safeGetClassName(packagePrefix, embeddable, "embeddable");
            enlistExplicitClass(collector, name);
        }
        for (var converter : mapping.getConverters()) {
            collector.potentialCdiBeanTypes.add(DotName.createSimple(qualifyIfNecessary(packagePrefix, converter.getClazz())));
        }
    }

    private void enlistOrmXmlMappingManagedClass(Collector collector, String packagePrefix,
            JaxbEntityOrMappedSuperclass managed,
            String nodeName) {
        String name = safeGetClassName(packagePrefix, managed, nodeName);
        enlistExplicitClass(collector, name);
        if (managed instanceof JaxbEntity entity) {
            // The call to 'enlistExplicitClass' above may not
            // detect that this class is an entity if it is not annotated
            collector.entityTypes.add(name);

            // Generators may be instantiated reflectively
            if (entity.getGenericGenerator() != null) {
                var generator = entity.getGenericGenerator();
                enlistPotentialClassReference(collector, generator == null ? null : generator.getClazz());
            }
        }

        enlistOrmXmlMappingListeners(collector, packagePrefix, managed.getEntityListenerContainer());
    }

    private void enlistOrmXmlMappingListeners(Collector collector, String packagePrefix,
            JaxbEntityListenerContainerImpl entityListeners) {
        if (entityListeners == null) {
            return;
        }
        for (var listener : entityListeners.getEntityListeners()) {
            collector.potentialCdiBeanTypes.add(DotName.createSimple(qualifyIfNecessary(packagePrefix, listener.getClazz())));
        }
    }

    private static String safeGetClassName(String packagePrefix, JaxbManagedType managedType, String nodeName) {
        String name = managedType.getClazz();
        if (name == null) {
            throw new IllegalArgumentException("Missing attribute '" + nodeName + ".class'");
        }
        return qualifyIfNecessary(packagePrefix, name);
    }

    // See org.hibernate.cfg.annotations.reflection.internal.XMLContext.buildSafeClassName(java.lang.String, java.lang.String)
    private static String qualifyIfNecessary(String packagePrefix, String name) {
        if (name.indexOf('.') < 0) {
            return packagePrefix + name;
        } else {
            // The class name is already qualified; don't apply the package prefix.
            return name;
        }
    }

    private void enlistHbmXmlMapping(Collector collector, JaxbHbmHibernateMapping mapping) {
        String packageValue = mapping.getPackage();
        String packagePrefix = packageValue == null ? "" : packageValue + ".";

        for (JaxbHbmRootEntityType entity : mapping.getClazz()) {
            enlistHbmXmlEntity(collector, packagePrefix, entity, entity.getAttributes());
            for (JaxbHbmDiscriminatorSubclassEntityType subclass : entity.getSubclass()) {
                enlistHbmXmlEntity(collector, packagePrefix, subclass, subclass.getAttributes());
            }
            for (JaxbHbmUnionSubclassEntityType subclass : entity.getUnionSubclass()) {
                enlistHbmXmlEntity(collector, packagePrefix, subclass, subclass.getAttributes());
            }
            for (JaxbHbmJoinedSubclassEntityType subclass : entity.getJoinedSubclass()) {
                enlistHbmXmlEntity(collector, packagePrefix, subclass, subclass.getAttributes());
            }
        }

        // For some reason the format also allows specifying subclasses at the top level.
        for (JaxbHbmDiscriminatorSubclassEntityType subclass : mapping.getSubclass()) {
            enlistHbmXmlEntity(collector, packagePrefix, subclass, subclass.getAttributes());
        }
        for (JaxbHbmUnionSubclassEntityType subclass : mapping.getUnionSubclass()) {
            enlistHbmXmlEntity(collector, packagePrefix, subclass, subclass.getAttributes());
        }
        for (JaxbHbmJoinedSubclassEntityType subclass : mapping.getJoinedSubclass()) {
            enlistHbmXmlEntity(collector, packagePrefix, subclass, subclass.getAttributes());
        }
    }

    private void enlistHbmXmlEntity(Collector collector, String packagePrefix,
            JaxbHbmEntityBaseDefinition entityDefinition, List<?> attributes) {
        String name = packagePrefix + entityDefinition.getName();
        enlistExplicitClass(collector, name);
        // The call to 'enlistExplicitClass' above may not
        // detect that this class is an entity if it is not annotated
        collector.entityTypes.add(name);
        collectHbmXmlEmbeddedTypes(collector, packagePrefix, attributes);
    }

    private void collectHbmXmlEmbeddedTypes(Collector collector, String packagePrefix, List<?> attributes) {
        for (Object attribute : attributes) {
            if (attribute instanceof JaxbHbmCompositeAttributeType) {
                JaxbHbmCompositeAttributeType compositeAttribute = (JaxbHbmCompositeAttributeType) attribute;
                String name = packagePrefix + compositeAttribute.getClazz();
                enlistExplicitClass(collector, name);
                // The call to 'enlistExplicitClass' above may not
                // detect that this class is an entity if it is not annotated
                collector.entityTypes.add(name);
                collectHbmXmlEmbeddedTypes(collector, packagePrefix, compositeAttribute.getAttributes());
            }
        }
    }

    private void enlistExplicitClass(Collector collector, String className) {
        DotName dotName = DotName.createSimple(className);
        // This will also take care of adding the class to unindexedClasses if necessary.
        addClassHierarchyToReflectiveList(collector, dotName);
    }

    private void enlistEmbeddedsAndElementCollections(Collector collector) throws BuildException {
        Set<DotName> embeddedTypes = new HashSet<>();

        for (DotName embeddedAnnotation : EMBEDDED_ANNOTATIONS) {
            for (AnnotationInstance annotation : index.getAnnotations(embeddedAnnotation)) {
                AnnotationTarget target = annotation.target();

                switch (target.kind()) {
                    case FIELD:
                        var field = target.asField();
                        collectEmbeddedType(embeddedTypes, field.declaringClass(), field, field.type(), true);
                        break;
                    case METHOD:
                        var method = target.asMethod();
                        if (method.isBridge()) {
                            // Generated by javac for covariant return type override.
                            // There's another method with a more specific return type, ignore this one.
                            continue;
                        }
                        collectEmbeddedType(embeddedTypes, method.declaringClass(), method, method.returnType(), true);
                        break;
                    default:
                        throw new IllegalStateException(
                                "[internal error] " + embeddedAnnotation + " placed on a unknown element: " + target);
                }

            }
        }

        for (AnnotationInstance annotation : index.getAnnotations(ClassNames.ELEMENT_COLLECTION)) {
            AnnotationTarget target = annotation.target();

            switch (target.kind()) {
                case FIELD:
                    var field = target.asField();
                    collectElementCollectionTypes(embeddedTypes, field.declaringClass(), field, field.type());
                    break;
                case METHOD:
                    var method = target.asMethod();
                    if (method.isBridge()) {
                        // Generated by javac for covariant return type override.
                        // There's another method with a more specific return type, ignore this one.
                        continue;
                    }
                    collectElementCollectionTypes(embeddedTypes, method.declaringClass(), method, method.returnType());
                    break;
                default:
                    throw new IllegalStateException(
                            "[internal error] " + ClassNames.ELEMENT_COLLECTION + " placed on a unknown element: " + target);
            }

        }

        for (DotName embeddedType : embeddedTypes) {
            addClassHierarchyToReflectiveList(collector, embeddedType);
        }
    }

    private void enlistJPAModelAnnotatedPackages(Collector collector, DotName dotName) {
        Collection<AnnotationInstance> jpaAnnotations = index.getAnnotations(dotName);

        if (jpaAnnotations == null) {
            return;
        }

        for (AnnotationInstance annotation : jpaAnnotations) {
            if (annotation.target().kind() != AnnotationTarget.Kind.CLASS) {
                continue; // Annotation on field, method, etc.
            }
            ClassInfo klass = annotation.target().asClass();
            if (!klass.simpleName().equals("package-info")) {
                continue; // Annotation on an actual class, not a package.
            }
            collectPackage(collector, klass);
        }
    }

    private void enlistJPAModelClasses(Collector collector, DotName dotName) {
        Collection<AnnotationInstance> jpaAnnotations = index.getAnnotations(dotName);

        if (jpaAnnotations == null) {
            return;
        }

        for (AnnotationInstance annotation : jpaAnnotations) {
            ClassInfo klass = annotation.target().asClass();
            DotName targetDotName = klass.name();
            addClassHierarchyToReflectiveList(collector, targetDotName);
            collectModelType(collector, klass);
        }
    }

    private void enlistJPAModelIdClasses(Collector collector, DotName dotName) {
        Collection<AnnotationInstance> jpaAnnotations = index.getAnnotations(dotName);

        if (jpaAnnotations == null) {
            return;
        }

        for (AnnotationInstance annotation : jpaAnnotations) {
            DotName targetDotName = annotation.value().asClass().name();
            addClassHierarchyToReflectiveList(collector, targetDotName);
            collector.modelTypes.add(targetDotName.toString());
        }
    }

    private void enlistPotentialCdiBeanClasses(Collector collector, DotName dotName) {
        Collection<AnnotationInstance> jpaAnnotations = index.getAnnotations(dotName);

        if (jpaAnnotations == null) {
            return;
        }

        for (AnnotationInstance annotation : jpaAnnotations) {
            AnnotationTarget target = annotation.target();
            ClassInfo beanType = switch (target.kind()) {
                case CLASS -> target.asClass();
                case FIELD -> target.asField().declaringClass();
                case METHOD -> target.asMethod().declaringClass();
                default -> throw new IllegalArgumentException(
                        "Annotation " + dotName + " was not expected on a target of kind " + target.kind());
            };
            DotName beanTypeDotName = beanType.name();
            collector.potentialCdiBeanTypes.add(beanTypeDotName);
        }
    }

    private void enlistPotentialClassReferences(Collector collector, DotName dotName, String... referenceAttributes) {
        Collection<AnnotationInstance> jpaAnnotations = index.getAnnotations(dotName);

        if (jpaAnnotations == null) {
            return;
        }

        for (AnnotationInstance annotation : jpaAnnotations) {
            for (String referenceAttribute : referenceAttributes) {
                var referenceValue = annotation.value(referenceAttribute);
                if (referenceValue == null) {
                    continue;
                }
                String reference = switch (referenceValue.kind()) {
                    case CLASS -> referenceValue.asClass().name().toString();
                    case STRING -> {
                        String stringRef = referenceValue.asString();
                        if (stringRef.isEmpty() || index.getClassByName(stringRef) == null) {
                            // No reference, or reference to a built-in strategy name like 'sequence'
                            // (which we can't resolve here and handle through GraalVMFeatures.registerGeneratorAndOptimizerClassesForReflections)
                            yield null;
                        }
                        yield stringRef;
                    }
                    default -> null;
                };
                enlistPotentialClassReference(collector, reference);
            }
        }
    }

    /**
     * Add the class to the reflective list with only constructor and method access.
     */
    private void enlistPotentialClassReference(Collector collector, String reference) {
        if (reference == null) {
            return;
        }
        collector.javaTypes.add(reference);
    }

    /**
     * Add the class to the reflective list with full method and field access.
     * Add the superclasses recursively as well as the interfaces.
     * Un-indexed classes/interfaces are accumulated to be thrown as a configuration error in the top level caller method
     * <p>
     * TODO should we also return the return types of all methods and fields? It could contain Enums for example.
     */
    private void addClassHierarchyToReflectiveList(Collector collector, DotName className) {
        if (className == null || isIgnored(className)) {
            // bail out if java.lang.Object or a class we want to ignore
            return;
        }

        // if the class is in the java. package and is not ignored, we want to register it for reflection
        if (isInJavaPackage(className)) {
            collector.javaTypes.add(className.toString());
            return;
        }

        ClassInfo classInfo = index.getClassByName(className);
        if (classInfo == null) {
            collector.unindexedClasses.add(className);
            return;
        }
        // we need to check for enums
        for (FieldInfo fieldInfo : classInfo.fields()) {
            Type fieldType = fieldInfo.type();
            if (Type.Kind.CLASS != fieldType.kind()) {
                // skip primitives and arrays
                continue;
            }
            DotName fieldClassName = fieldInfo.type().name();
            ClassInfo fieldTypeClassInfo = index.getClassByName(fieldClassName);
            if (fieldTypeClassInfo != null && ClassNames.ENUM.equals(fieldTypeClassInfo.superName())) {
                collector.enumTypes.add(fieldClassName.toString());
            }
        }

        //Capture this one (for various needs: Reflective access enablement, Hibernate enhancement, JPA Template)
        collectModelType(collector, classInfo);

        // add superclass recursively
        addClassHierarchyToReflectiveList(collector, classInfo.superName());
        // add interfaces recursively
        for (DotName interfaceDotName : classInfo.interfaceNames()) {
            addClassHierarchyToReflectiveList(collector, interfaceDotName);
        }
    }

    private static void collectPackage(Collector collector, ClassInfo classOrPackageInfo) {
        String classOrPackageInfoName = classOrPackageInfo.name().toString();
        String packageName = classOrPackageInfoName.substring(0, classOrPackageInfoName.lastIndexOf('.'));
        collector.packages.add(packageName);
    }

    private static void collectModelType(Collector collector, ClassInfo modelClass) {
        String name = modelClass.name().toString();
        collector.modelTypes.add(name);
        if (modelClass.declaredAnnotation(ClassNames.JPA_ENTITY) != null) {
            collector.entityTypes.add(name);
        }
    }

    private void collectEmbeddedType(Set<DotName> embeddedTypes, ClassInfo declaringClass,
            Declaration attribute, Type attributeType, boolean validate)
            throws BuildException {
        DotName className;
        switch (attributeType.kind()) {
            case CLASS:
                className = attributeType.asClassType().name();
                break;
            case PARAMETERIZED_TYPE:
                className = attributeType.name();
                break;
            default:
                // do nothing
                return;
        }
        if (validate && !index.getClassByName(className).hasAnnotation(ClassNames.EMBEDDABLE)) {
            throw new BuildException(
                    "Type " + className + " must be annotated with @Embeddable, because it is used as an embeddable."
                            + " This type is used in class " + declaringClass
                            + " for attribute " + attribute + ".");
        }
        embeddedTypes.add(attributeType.name());
    }

    private void collectElementCollectionTypes(Set<DotName> embeddedTypes, ClassInfo declaringClass,
            Declaration attribute, Type attributeType)
            throws BuildException {
        switch (attributeType.kind()) {
            case CLASS:
                // Raw collection type, nothing we can do
                break;
            case PARAMETERIZED_TYPE:
                embeddedTypes.add(attributeType.name());
                var typeArguments = attributeType.asParameterizedType().arguments();
                for (Type typeArgument : typeArguments) {
                    // We don't validate @Embeddable annotations on element collections at the moment
                    // See https://github.com/quarkusio/quarkus/pull/35822
                    collectEmbeddedType(embeddedTypes, declaringClass, attribute, typeArgument, false);
                }
                break;
            case ARRAY:
                collectEmbeddedType(embeddedTypes, declaringClass, attribute, attributeType.asArrayType().constituent(), true);
                break;
            default:
                // do nothing
                break;
        }
    }

    private static boolean isIgnored(DotName classDotName) {
        String className = classDotName.toString();
        if (className.startsWith("java.util.") || className.startsWith("java.lang.")
                || className.startsWith("org.hibernate.engine.spi.")
                || className.startsWith("jakarta.persistence.")
                || className.startsWith("jakarta.persistence.")) {
            return true;
        }
        return false;
    }

    private static boolean isInJavaPackage(DotName classDotName) {
        String className = classDotName.toString();
        if (className.startsWith("java.")) {
            return true;
        }
        return false;
    }

    private static class Collector {
        final Set<String> packages = new HashSet<>();
        final Set<String> entityTypes = new HashSet<>();
        final Set<DotName> potentialCdiBeanTypes = new HashSet<>();
        final Set<String> modelTypes = new HashSet<>();
        final Set<String> enumTypes = new HashSet<>();
        final Set<String> javaTypes = new HashSet<>();
        final Set<DotName> unindexedClasses = new HashSet<>();
        final Map<String, List<RecordableXmlMapping>> xmlMappingsByPU = new HashMap<>();
    }
}
