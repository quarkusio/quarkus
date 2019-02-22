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

package io.quarkus.hibernate.orm;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;

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

    public JpaEntitiesBuildItems discoverModelAndRegisterForReflection() throws IOException {
        // list all entities and create a JPADeploymentTemplate out of it
        // Not functional as we will need one deployment template per persistence unit
        final JpaEntitiesBuildItems domainObjectCollector = new JpaEntitiesBuildItems();
        final Set<String> enumTypeCollector = new HashSet<>();

        enlistJPAModelClasses(indexView, domainObjectCollector, enumTypeCollector, JPA_ENTITY);
        enlistJPAModelClasses(indexView, domainObjectCollector, enumTypeCollector, EMBEDDABLE);
        enlistJPAModelClasses(indexView, domainObjectCollector, enumTypeCollector, MAPPED_SUPERCLASS);
        enlistReturnType(indexView, domainObjectCollector, enumTypeCollector);

        for (PersistenceUnitDescriptor pud : descriptors) {
            enlistExplicitClasses(indexView, domainObjectCollector, enumTypeCollector, pud.getManagedClassNames());
        }

        domainObjectCollector.registerAllForReflection(reflectiveClass);

        if (!enumTypeCollector.isEmpty()) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, Enum.class.getName()));
            for (String className : enumTypeCollector) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, className));
            }
        }

        return domainObjectCollector;
    }

    private static void enlistExplicitClasses(IndexView index, JpaEntitiesBuildItems domainObjectCollector,
            Set<String> enumTypeCollector, List<String> managedClassNames) {
        for (String className : managedClassNames) {
            DotName dotName = DotName.createSimple(className);
            boolean isInIndex = index.getClassByName(dotName) != null;
            if (isInIndex) {
                addClassHierarchyToReflectiveList(index, domainObjectCollector, enumTypeCollector, dotName);
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

    private static void enlistReturnType(IndexView index, JpaEntitiesBuildItems domainObjectCollector,
            Set<String> enumTypeCollector) {
        Collection<AnnotationInstance> annotations = index.getAnnotations(EMBEDDED);
        if (annotations != null && annotations.size() > 0) {
            for (AnnotationInstance annotation : annotations) {
                AnnotationTarget target = annotation.target();
                DotName jpaClassName = null;
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
                addClassHierarchyToReflectiveList(index, domainObjectCollector, enumTypeCollector, jpaClassName);
            }
        }
    }

    private void enlistJPAModelClasses(IndexView index, JpaEntitiesBuildItems domainObjectCollector,
            Set<String> enumTypeCollector, DotName dotName) {
        Collection<AnnotationInstance> jpaAnnotations = index.getAnnotations(dotName);
        if (jpaAnnotations != null && jpaAnnotations.size() > 0) {
            for (AnnotationInstance annotation : jpaAnnotations) {
                ClassInfo klass = annotation.target().asClass();
                DotName targetDotName = klass.name();
                // ignore non-jpa model classes that we think belong to JPA
                if (nonJpaModelClasses.contains(targetDotName.toString())) {
                    continue;
                }
                addClassHierarchyToReflectiveList(index, domainObjectCollector, enumTypeCollector, targetDotName);
                domainObjectCollector.addEntity(targetDotName.toString());
            }
        }
    }

    /**
     * Add the class to the reflective list with full method and field access.
     * Add the superclasses recursively as well as the interfaces.
     * <p>
     * TODO this approach fails if the Jandex index is not complete (e.g. misses somes interface or super types)
     * TODO should we also return the return types of all methods and fields? It could container Enums for example.
     */
    private static void addClassHierarchyToReflectiveList(IndexView index, JpaEntitiesBuildItems domainObjectCollector,
            Set<String> enumTypeCollector, DotName className) {
        // If type is not Object
        // recursively add superclass and interfaces
        if (className == null) {
            // java.lang.Object
            return;
        }
        ClassInfo classInfo = index.getClassByName(className);
        if (classInfo == null) {
            if (className.equals(ClassType.OBJECT_TYPE.name()) || className.toString().equals(Serializable.class.getName())) {
                return;
            } else {
                throw new IllegalStateException("The Jandex index is not complete, missing: " + className.toString());
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
        addClassHierarchyToReflectiveList(index, domainObjectCollector, enumTypeCollector, classInfo.superName());
        // add interfaces recursively
        for (DotName interfaceDotName : classInfo.interfaceNames()) {
            addClassHierarchyToReflectiveList(index, domainObjectCollector, enumTypeCollector, interfaceDotName);
        }
    }
}
