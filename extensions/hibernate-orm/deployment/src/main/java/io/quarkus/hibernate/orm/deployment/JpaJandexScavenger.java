package io.quarkus.hibernate.orm.deployment;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.configuration.ConfigurationError;

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

    private final List<PersistenceXmlDescriptorBuildItem> explicitDescriptors;
    private final BuildProducer<ReflectiveClassBuildItem> reflectiveClass;
    private final IndexView indexView;
    private final Set<String> nonJpaModelClasses;
    private final Set<String> ignorableNonIndexedClasses;

    JpaJandexScavenger(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            List<PersistenceXmlDescriptorBuildItem> explicitDescriptors,
            IndexView indexView,
            Set<String> nonJpaModelClasses,
            Set<String> ignorableNonIndexedClasses) {
        this.reflectiveClass = reflectiveClass;
        this.explicitDescriptors = explicitDescriptors;
        this.indexView = indexView;
        this.nonJpaModelClasses = nonJpaModelClasses;
        this.ignorableNonIndexedClasses = ignorableNonIndexedClasses;
    }

    public JpaEntitiesBuildItem discoverModelAndRegisterForReflection() throws IOException {
        // list all entities and create a JPADeploymentTemplate out of it
        // Not functional as we will need one deployment template per persistence unit
        final JpaEntitiesBuildItem domainObjectCollector = new JpaEntitiesBuildItem();
        final Set<String> enumTypeCollector = new HashSet<>();
        final Set<String> javaTypeCollector = new HashSet<>();
        final Set<DotName> unindexedClasses = new TreeSet<>();

        for (DotName packageAnnotation : HibernateOrmAnnotations.PACKAGE_ANNOTATIONS) {
            enlistJPAModelAnnotatedPackages(indexView, domainObjectCollector, packageAnnotation);
        }
        enlistJPAModelClasses(indexView, domainObjectCollector, enumTypeCollector, javaTypeCollector, ClassNames.JPA_ENTITY,
                unindexedClasses);
        enlistJPAModelClasses(indexView, domainObjectCollector, enumTypeCollector, javaTypeCollector, ClassNames.EMBEDDABLE,
                unindexedClasses);
        enlistJPAModelClasses(indexView, domainObjectCollector, enumTypeCollector, javaTypeCollector,
                ClassNames.MAPPED_SUPERCLASS,
                unindexedClasses);
        enlistEmbeddedsAndElementCollections(indexView, domainObjectCollector, enumTypeCollector, javaTypeCollector,
                unindexedClasses);

        for (PersistenceXmlDescriptorBuildItem pud : explicitDescriptors) {
            final List<String> managedClassNames = pud.getDescriptor().getManagedClassNames();
            enlistExplicitClasses(indexView, domainObjectCollector, enumTypeCollector, javaTypeCollector,
                    managedClassNames, unindexedClasses);
        }

        domainObjectCollector.registerAllForReflection(reflectiveClass);

        if (!enumTypeCollector.isEmpty()) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, "java.lang.Enum"));
            for (String className : enumTypeCollector) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, className));
            }
        }

        // for the java. types we collected (usually from java.time but it could be from other types),
        // we just register them for reflection
        for (String javaType : javaTypeCollector) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, javaType));
        }

        if (!unindexedClasses.isEmpty()) {
            Set<String> unIgnorableIndexedClasses = unindexedClasses.stream().map(DotName::toString)
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

        return domainObjectCollector;
    }

    private static void enlistExplicitClasses(IndexView index, JpaEntitiesBuildItem domainObjectCollector,
            Set<String> enumTypeCollector, Set<String> javaTypeCollector, List<String> managedClassNames,
            Set<DotName> unindexedClasses) {
        for (String className : managedClassNames) {
            DotName dotName = DotName.createSimple(className);
            boolean isInIndex = index.getClassByName(dotName) != null;
            if (!isInIndex) {
                unindexedClasses.add(dotName);
            }

            addClassHierarchyToReflectiveList(index, domainObjectCollector, enumTypeCollector, javaTypeCollector, dotName,
                    unindexedClasses);
        }
    }

    private static void enlistEmbeddedsAndElementCollections(IndexView index, JpaEntitiesBuildItem domainObjectCollector,
            Set<String> enumTypeCollector, Set<String> javaTypeCollector, Set<DotName> unindexedClasses) {
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
            addClassHierarchyToReflectiveList(index, domainObjectCollector, enumTypeCollector, javaTypeCollector, embeddedType,
                    unindexedClasses);
        }
    }

    private void enlistJPAModelAnnotatedPackages(IndexView index, JpaEntitiesBuildItem domainObjectCollector, DotName dotName) {
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
            DotName targetDotName = klass.name();
            // ignore non-jpa model classes that we think belong to JPA
            if (nonJpaModelClasses.contains(targetDotName.toString())) {
                continue;
            }
            collectPackage(domainObjectCollector, klass);
        }
    }

    private void enlistJPAModelClasses(IndexView index, JpaEntitiesBuildItem domainObjectCollector,
            Set<String> enumTypeCollector, Set<String> javaTypeCollector, DotName dotName, Set<DotName> unindexedClasses) {
        Collection<AnnotationInstance> jpaAnnotations = index.getAnnotations(dotName);

        if (jpaAnnotations == null) {
            return;
        }

        for (AnnotationInstance annotation : jpaAnnotations) {
            ClassInfo klass = annotation.target().asClass();
            DotName targetDotName = klass.name();
            // ignore non-jpa model classes that we think belong to JPA
            if (nonJpaModelClasses.contains(targetDotName.toString())) {
                continue;
            }
            addClassHierarchyToReflectiveList(index, domainObjectCollector, enumTypeCollector, javaTypeCollector, targetDotName,
                    unindexedClasses);
            collectDomainObject(domainObjectCollector, klass);
        }
    }

    /**
     * Add the class to the reflective list with full method and field access.
     * Add the superclasses recursively as well as the interfaces.
     * Un-indexed classes/interfaces are accumulated to be thrown as a configuration error in the top level caller method
     * <p>
     * TODO should we also return the return types of all methods and fields? It could contain Enums for example.
     */
    private static void addClassHierarchyToReflectiveList(IndexView index, JpaEntitiesBuildItem domainObjectCollector,
            Set<String> enumTypeCollector, Set<String> javaTypeCollector, DotName className, Set<DotName> unindexedClasses) {
        if (className == null || isIgnored(className)) {
            // bail out if java.lang.Object or a class we want to ignore
            return;
        }

        // if the class is in the java. package and is not ignored, we want to register it for reflection
        if (isInJavaPackage(className)) {
            javaTypeCollector.add(className.toString());
            return;
        }

        ClassInfo classInfo = index.getClassByName(className);
        if (classInfo == null) {
            unindexedClasses.add(className);
            return;
        }
        // we need to check for enums
        for (FieldInfo fieldInfo : classInfo.fields()) {
            DotName fieldType = fieldInfo.type().name();
            ClassInfo fieldTypeClassInfo = index.getClassByName(fieldType);
            if (fieldTypeClassInfo != null && ClassNames.ENUM.equals(fieldTypeClassInfo.superName())) {
                enumTypeCollector.add(fieldType.toString());
            }
        }

        //Capture this one (for various needs: Reflective access enablement, Hibernate enhancement, JPA Template)
        collectDomainObject(domainObjectCollector, classInfo);

        // add superclass recursively
        addClassHierarchyToReflectiveList(index, domainObjectCollector, enumTypeCollector, javaTypeCollector,
                classInfo.superName(),
                unindexedClasses);
        // add interfaces recursively
        for (DotName interfaceDotName : classInfo.interfaceNames()) {
            addClassHierarchyToReflectiveList(index, domainObjectCollector, enumTypeCollector, javaTypeCollector,
                    interfaceDotName,
                    unindexedClasses);
        }
    }

    private static void collectPackage(JpaEntitiesBuildItem domainObjectCollector, ClassInfo classOrPackageInfo) {
        String classOrPackageInfoName = classOrPackageInfo.name().toString();
        String packageName = classOrPackageInfoName.substring(0, classOrPackageInfoName.lastIndexOf('.'));
        domainObjectCollector.addModelPackage(packageName);
    }

    private static void collectDomainObject(JpaEntitiesBuildItem domainObjectCollector, ClassInfo modelClass) {
        if (modelClass.classAnnotation(ClassNames.JPA_ENTITY) != null) {
            domainObjectCollector.addEntityClass(modelClass.name().toString());
        } else {
            domainObjectCollector.addModelClass(modelClass.name().toString());
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
}
