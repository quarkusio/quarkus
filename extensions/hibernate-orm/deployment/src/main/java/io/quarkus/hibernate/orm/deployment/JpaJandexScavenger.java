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
import org.hibernate.boot.jaxb.mapping.spi.EntityOrMappedSuperclass;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConverter;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddable;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntity;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListener;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListeners;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappings;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMappedSuperclass;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitDefaults;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitMetadata;
import org.hibernate.boot.jaxb.mapping.spi.ManagedType;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.hibernate.orm.deployment.xml.QuarkusMappingFileParser;
import io.quarkus.hibernate.orm.runtime.boot.xml.RecordableXmlMapping;

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

    public static final List<DotName> EMBEDDED_ANNOTATIONS = Arrays.asList(ClassNames.EMBEDDED, ClassNames.ELEMENT_COLLECTION);

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

    public JpaModelBuildItem discoverModelAndRegisterForReflection() {
        Collector collector = new Collector();

        for (DotName packageAnnotation : HibernateOrmAnnotations.PACKAGE_ANNOTATIONS) {
            enlistJPAModelAnnotatedPackages(collector, packageAnnotation);
        }
        enlistJPAModelClasses(collector, ClassNames.JPA_ENTITY);
        enlistJPAModelClasses(collector, ClassNames.EMBEDDABLE);
        enlistJPAModelClasses(collector, ClassNames.MAPPED_SUPERCLASS);
        enlistEmbeddedsAndElementCollections(collector);

        enlistPotentialCdiBeanClasses(collector, ClassNames.CONVERTER);
        for (DotName annotation : HibernateOrmAnnotations.JPA_LISTENER_ANNOTATIONS) {
            enlistPotentialCdiBeanClasses(collector, annotation);
        }

        for (JpaModelPersistenceUnitContributionBuildItem persistenceUnitContribution : persistenceUnitContributions) {
            enlistExplicitMappings(collector, persistenceUnitContribution);
        }

        Set<String> allModelClassNames = new HashSet<>(collector.entityTypes);
        allModelClassNames.addAll(collector.modelTypes);
        for (String className : allModelClassNames) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, className));
        }

        if (!collector.enumTypes.isEmpty()) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, "java.lang.Enum"));
            for (String className : collector.enumTypes) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, className));
            }
        }

        // for the java types we collected (usually from java.time but it could be from other types),
        // we just register them for reflection
        for (String javaType : collector.javaTypes) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, javaType));
        }

        // Converters need to be in the list of model types in order for @Converter#autoApply to work,
        // but they don't need reflection enabled.
        for (DotName potentialCdiBeanType : collector.potentialCdiBeanTypes) {
            allModelClassNames.add(potentialCdiBeanType.toString());
        }

        if (!collector.unindexedClasses.isEmpty()) {
            Set<String> unIgnorableIndexedClasses = collector.unindexedClasses.stream().map(DotName::toString)
                    .collect(Collectors.toSet());
            unIgnorableIndexedClasses.removeAll(ignorableNonIndexedClasses);

            if (!unIgnorableIndexedClasses.isEmpty()) {
                final String unindexedClassesErrorMessage = unIgnorableIndexedClasses.stream().map(d -> "\t- " + d + "\n")
                        .collect(Collectors.joining());
                throw new ConfigurationError(
                        "Unable to properly register the hierarchy of the following JPA classes as they are not in the Jandex index:\n"
                                + unindexedClassesErrorMessage
                                + "Consider adding them to the index either by creating a Jandex index " +
                                "for your dependency via the Maven plugin, an empty META-INF/beans.xml or quarkus.index-dependency properties.");
            }
        }

        return new JpaModelBuildItem(collector.packages, collector.entityTypes, collector.potentialCdiBeanTypes,
                allModelClassNames, collector.xmlMappingsByPU);
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

    private void enlistOrmXmlMapping(Collector collector, JaxbEntityMappings mapping) {
        String packageName = mapping.getPackage();
        String packagePrefix = packageName == null ? "" : packageName + ".";

        JaxbPersistenceUnitMetadata metadata = mapping.getPersistenceUnitMetadata();
        JaxbPersistenceUnitDefaults defaults = metadata == null ? null : metadata.getPersistenceUnitDefaults();
        if (defaults != null) {
            enlistOrmXmlMappingListeners(collector, packagePrefix, defaults.getEntityListeners());
        }

        for (JaxbEntity entity : mapping.getEntity()) {
            enlistOrmXmlMappingManagedClass(collector, packagePrefix, entity, "entity");
        }
        for (JaxbMappedSuperclass mappedSuperclass : mapping.getMappedSuperclass()) {
            enlistOrmXmlMappingManagedClass(collector, packagePrefix, mappedSuperclass, "mapped-superclass");
        }
        for (JaxbEmbeddable embeddable : mapping.getEmbeddable()) {
            String name = safeGetClassName(packagePrefix, embeddable, "embeddable");
            enlistExplicitClass(collector, name);
        }
        for (JaxbConverter converter : mapping.getConverter()) {
            collector.potentialCdiBeanTypes.add(DotName.createSimple(qualifyIfNecessary(packagePrefix, converter.getClazz())));
        }
    }

    private void enlistOrmXmlMappingManagedClass(Collector collector, String packagePrefix, EntityOrMappedSuperclass managed,
            String nodeName) {
        String name = safeGetClassName(packagePrefix, managed, nodeName);
        enlistExplicitClass(collector, name);
        if (managed instanceof JaxbEntity) {
            // The call to 'enlistExplicitClass' above may not
            // detect that this class is an entity if it is not annotated
            collector.entityTypes.add(name);
        }

        enlistOrmXmlMappingListeners(collector, packagePrefix, managed.getEntityListeners());
    }

    private void enlistOrmXmlMappingListeners(Collector collector, String packagePrefix, JaxbEntityListeners entityListeners) {
        if (entityListeners == null) {
            return;
        }
        for (JaxbEntityListener listener : entityListeners.getEntityListener()) {
            collector.potentialCdiBeanTypes.add(DotName.createSimple(qualifyIfNecessary(packagePrefix, listener.getClazz())));
        }
    }

    private static String safeGetClassName(String packagePrefix, ManagedType managedType, String nodeName) {
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
                collectHbmXmlEmbeddedTypes(collector, packagePrefix, attributes);
            }
        }
    }

    private void enlistExplicitClass(Collector collector, String className) {
        DotName dotName = DotName.createSimple(className);
        // This will also take care of adding the class to unindexedClasses if necessary.
        addClassHierarchyToReflectiveList(collector, dotName);
    }

    private void enlistEmbeddedsAndElementCollections(Collector collector) {
        Set<DotName> embeddedTypes = new HashSet<>();

        for (DotName embeddedAnnotation : EMBEDDED_ANNOTATIONS) {
            Collection<AnnotationInstance> annotations = index.getAnnotations(embeddedAnnotation);

            for (AnnotationInstance annotation : annotations) {
                AnnotationTarget target = annotation.target();

                switch (target.kind()) {
                    case FIELD:
                        collectEmbeddedTypes(embeddedTypes, target.asField().type());
                        break;
                    case METHOD:
                        collectEmbeddedTypes(embeddedTypes, target.asMethod().returnType());
                        break;
                    default:
                        throw new IllegalStateException(
                                "[internal error] " + embeddedAnnotation + " placed on a unknown element: " + target);
                }

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

    private void enlistPotentialCdiBeanClasses(Collector collector, DotName dotName) {
        Collection<AnnotationInstance> jpaAnnotations = index.getAnnotations(dotName);

        if (jpaAnnotations == null) {
            return;
        }

        for (AnnotationInstance annotation : jpaAnnotations) {
            AnnotationTarget target = annotation.target();
            ClassInfo beanType;
            switch (target.kind()) {
                case CLASS:
                    beanType = target.asClass();
                    break;
                case FIELD:
                    beanType = target.asField().declaringClass();
                    break;
                case METHOD:
                    beanType = target.asMethod().declaringClass();
                    break;
                case METHOD_PARAMETER:
                case TYPE:
                case RECORD_COMPONENT:
                default:
                    throw new IllegalArgumentException(
                            "Annotation " + dotName + " was not expected on a target of kind " + target.kind());
            }
            DotName beanTypeDotName = beanType.name();
            collector.potentialCdiBeanTypes.add(beanTypeDotName);
        }
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
        if (modelClass.classAnnotation(ClassNames.JPA_ENTITY) != null) {
            collector.entityTypes.add(name);
        }
    }

    private static void collectEmbeddedTypes(Set<DotName> embeddedTypes, Type indexType) {
        switch (indexType.kind()) {
            case CLASS:
                embeddedTypes.add(indexType.asClassType().name());
                break;
            case PARAMETERIZED_TYPE:
                embeddedTypes.add(indexType.name());
                for (Type typeArgument : indexType.asParameterizedType().arguments()) {
                    collectEmbeddedTypes(embeddedTypes, typeArgument);
                }
                break;
            case ARRAY:
                collectEmbeddedTypes(embeddedTypes, indexType.asArrayType().component());
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
                || className.startsWith("javax.persistence.")) {
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
