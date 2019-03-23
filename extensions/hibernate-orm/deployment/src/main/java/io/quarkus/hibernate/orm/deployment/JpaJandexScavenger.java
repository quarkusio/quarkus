/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.hibernate.orm.deployment;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
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
final class JpaJandexScavenger {

    private static final DotName JPA_ENTITY = DotName.createSimple(Entity.class.getName());
    private static final DotName EMBEDDABLE = DotName.createSimple(Embeddable.class.getName());
    private static final DotName EMBEDDED = DotName.createSimple(Embedded.class.getName());
    private static final DotName MAPPED_SUPERCLASS = DotName.createSimple(MappedSuperclass.class.getName());

    private static final DotName ENUM = DotName.createSimple(Enum.class.getName());
    private static final Logger log = Logger.getLogger("io.quarkus.hibernate.orm");

    private final List<ParsedPersistenceXmlDescriptor> descriptors;
    private final BuildProducer<ReflectiveClassBuildItem> reflectiveClass;
    private final IndexView indexView;
    private final Set<String> nonJpaModelClasses;

    JpaJandexScavenger(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            List<ParsedPersistenceXmlDescriptor> descriptors,
            IndexView indexView,
            Set<String> nonJpaModelClasses) {
        this.reflectiveClass = reflectiveClass;
        this.descriptors = descriptors;
        this.indexView = indexView;
        this.nonJpaModelClasses = nonJpaModelClasses;
    }

    public JpaEntitiesBuildItem discoverModelAndRegisterForReflection() throws IOException {
        // list all entities and create a JPADeploymentTemplate out of it
        // Not functional as we will need one deployment template per persistence unit
        final JpaEntitiesBuildItem domainObjectCollector = new JpaEntitiesBuildItem();
        final Set<String> enumTypeCollector = new HashSet<>();
        final Set<DotName> unindexedClasses = new HashSet<>();

        enlistJPAModelClasses(indexView, domainObjectCollector, enumTypeCollector, JPA_ENTITY, unindexedClasses);
        enlistJPAModelClasses(indexView, domainObjectCollector, enumTypeCollector, EMBEDDABLE, unindexedClasses);
        enlistJPAModelClasses(indexView, domainObjectCollector, enumTypeCollector, MAPPED_SUPERCLASS, unindexedClasses);
        enlistReturnType(indexView, domainObjectCollector, enumTypeCollector, unindexedClasses);

        for (PersistenceUnitDescriptor pud : descriptors) {
            enlistExplicitClasses(indexView, domainObjectCollector, enumTypeCollector, pud.getManagedClassNames(),
                    unindexedClasses);
        }

        domainObjectCollector.registerAllForReflection(reflectiveClass);

        if (!enumTypeCollector.isEmpty()) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, Enum.class.getName()));
            for (String className : enumTypeCollector) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, className));
            }
        }

        if (!unindexedClasses.isEmpty()) {
            final String unindexedClassesErrorMessage = unindexedClasses.stream().map(d -> "\t- " + d + "\n")
                    .collect(Collectors.joining());
            throw new ConfigurationError(
                    "Unable to properly register the hierarchy of the following classes for reflection as they are not in the Jandex index:\n"
                            + unindexedClassesErrorMessage
                            + "Consider adding them to the index either by creating a Jandex index " +
                            "for your dependency or via quarkus.index-dependency properties.");
        }

        return domainObjectCollector;
    }

    private static void enlistExplicitClasses(IndexView index, JpaEntitiesBuildItem domainObjectCollector,
            Set<String> enumTypeCollector, List<String> managedClassNames, Set<DotName> unindexedClasses) {
        for (String className : managedClassNames) {
            DotName dotName = DotName.createSimple(className);
            boolean isInIndex = index.getClassByName(dotName) != null;
            if (isInIndex) {
                addClassHierarchyToReflectiveList(index, domainObjectCollector, enumTypeCollector, dotName, unindexedClasses);
            } else {
                // We do lipstick service by manually adding explicitly the <class> reference but not navigating the hierarchy
                // so a class with a complex hierarchy will fail.
                log.warnf(
                        "Did not find `%s` in the indexed jars. You likely forgot to tell Quarkus to index your dependency jar. See https://github.com/quarkus-project/quarkus/#indexing-and-application-classes for more info.",
                        className);
                domainObjectCollector.addEntity(className);
            }
        }
    }

    private static void enlistReturnType(IndexView index, JpaEntitiesBuildItem domainObjectCollector,
            Set<String> enumTypeCollector, Set<DotName> unindexedClasses) {
        Collection<AnnotationInstance> annotations = index.getAnnotations(EMBEDDED);
        if (annotations == null) {
            return;
        }

        for (AnnotationInstance annotation : annotations) {
            AnnotationTarget target = annotation.target();
            DotName jpaClassName;
            switch (target.kind()) {
                case FIELD:
                    // TODO could fail if that's an array or a generic type
                    jpaClassName = target.asField().type().name();
                    break;
                case METHOD:
                    // TODO could fail if that's an array or a generic type
                    jpaClassName = target.asMethod().returnType().name();
                    break;
                default:
                    throw new IllegalStateException("[internal error] @Embedded placed on a unknown element: " + target);
            }
            addClassHierarchyToReflectiveList(index, domainObjectCollector, enumTypeCollector, jpaClassName,
                    unindexedClasses);
        }
    }

    private void enlistJPAModelClasses(IndexView index, JpaEntitiesBuildItem domainObjectCollector,
            Set<String> enumTypeCollector, DotName dotName, Set<DotName> unindexedClasses) {
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
            addClassHierarchyToReflectiveList(index, domainObjectCollector, enumTypeCollector, targetDotName,
                    unindexedClasses);
            domainObjectCollector.addEntity(targetDotName.toString());
        }
    }

    /**
     * Add the class to the reflective list with full method and field access.
     * Add the superclasses recursively as well as the interfaces.
     * Un-indexed classes/interfaces are accumulated to be thrown as a configuration error in the top level caller method
     * <p>
     * TODO should we also return the return types of all methods and fields? It could container Enums for example.
     */
    private static void addClassHierarchyToReflectiveList(IndexView index, JpaEntitiesBuildItem domainObjectCollector,
            Set<String> enumTypeCollector, DotName className, Set<DotName> unindexedClasses) {
        // If type is not Object
        // recursively add superclass and interfaces
        if (className == null) {
            // java.lang.Object
            return;
        }

        ClassInfo classInfo = index.getClassByName(className);
        if (classInfo == null) {
            if (className.toString().startsWith("java.")) {
                return;
            } else {
                unindexedClasses.add(className);
                return;
            }
        }
        //we need to check for enums
        for (FieldInfo fieldInfo : classInfo.fields()) {
            DotName fieldType = fieldInfo.type().name();
            ClassInfo fieldTypeClassInfo = index.getClassByName(fieldType);
            if (fieldTypeClassInfo != null && ENUM.equals(fieldTypeClassInfo.superName())) {
                enumTypeCollector.add(fieldType.toString());
            }
        }

        //Capture this one (for various needs: Reflective access enablement, Hibernate enhancement, JPA Template)
        domainObjectCollector.addEntity(className.toString());
        // add superclass recursively
        addClassHierarchyToReflectiveList(index, domainObjectCollector, enumTypeCollector, classInfo.superName(),
                unindexedClasses);
        // add interfaces recursively
        for (DotName interfaceDotName : classInfo.interfaceNames()) {
            addClassHierarchyToReflectiveList(index, domainObjectCollector, enumTypeCollector, interfaceDotName,
                    unindexedClasses);
        }
    }
}
