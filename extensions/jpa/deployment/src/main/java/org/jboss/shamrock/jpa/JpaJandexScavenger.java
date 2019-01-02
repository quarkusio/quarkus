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

package org.jboss.shamrock.jpa;

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
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.jpa.runtime.JPADeploymentTemplate;

/**
 * Scan the Jandex index to find JPA entities (and embeddables supporting entity models).
 * <p>
 * The output is then both consumed as plain list to use as a filter for which classes
 * need to be enhanced, collect them for storage in the JPADeploymentTemplate and registered
 * for reflective access.
 * TODO some of these are going to be redundant?
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Sanne Grinovero  <sanne@hibernate.org>
 */
final class JpaJandexScavenger {

    private static final DotName JPA_ENTITY = DotName.createSimple(Entity.class.getName());
    private static final DotName EMBEDDABLE = DotName.createSimple(Embeddable.class.getName());
    private static final DotName EMBEDDED = DotName.createSimple(Embedded.class.getName());
    private static final DotName MAPPED_SUPERCLASS = DotName.createSimple(MappedSuperclass.class.getName());

    private static final DotName ENUM = DotName.createSimple(Enum.class.getName());
    private static final Logger log = Logger.getLogger("org.jboss.shamrock.jpa");

    private final List<ParsedPersistenceXmlDescriptor> descriptors;
    private final BuildProducer<ReflectiveClassBuildItem> reflectiveClass;
    private final IndexView combinedIndex;
    private final JPADeploymentTemplate template;

    JpaJandexScavenger(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, List<ParsedPersistenceXmlDescriptor> descriptors, IndexView combinedIndex, JPADeploymentTemplate template) {
        this.reflectiveClass = reflectiveClass;
        this.descriptors = descriptors;
        this.combinedIndex = combinedIndex;
        this.template = template;
    }

    public KnownDomainObjects discoverModelAndRegisterForReflection() throws IOException {
        // list all entities and create a JPADeploymentTemplate out of it
        // Not functional as we will need one deployment template per persistence unit
        final IndexView index = combinedIndex;
        final DomainObjectSet collector = new DomainObjectSet();

        enlistJPAModelClasses(JPA_ENTITY, collector, index);
        enlistJPAModelClasses(EMBEDDABLE, collector, index);
        enlistJPAModelClasses(MAPPED_SUPERCLASS, collector, index);
        enlistReturnType(collector, index);

        for (PersistenceUnitDescriptor pud : descriptors) {
            enlistExplicitClasses(pud.getManagedClassNames(), collector, index);
        }

        collector.registerAllForReflection(reflectiveClass);

        collector.dumpAllToJPATemplate(template);
        template.enlistPersistenceUnit();
        template.callHibernateFeatureInit();

        return collector;
    }

    private static void enlistExplicitClasses(List<String> managedClassNames, DomainObjectSet collector, IndexView index) {
        for (String className : managedClassNames) {
            DotName dotName = DotName.createSimple(className);
            boolean isInIndex = index.getClassByName(dotName) != null;
            if (isInIndex) {
                addClassHierarchyToReflectiveList(collector, index, dotName);
            } else {
                // We do lipstick service by manually adding explicitly the <class> reference but not navigating the hierarchy
                // so a class with a complex hierarchy will fail.
                log.warnf("Did not find `%s` in the indexed jars. You likely forgot to tell Shamrock to index your dependency jar. See https://github.com/protean-project/shamrock/#indexing-and-application-classes for more info.", className);
                collector.addEntity(className);
            }
        }
    }

    private static void enlistReturnType(DomainObjectSet collector, IndexView index) {
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
                addClassHierarchyToReflectiveList(collector, index, jpaClassName);
            }
        }
    }

    private static void enlistJPAModelClasses(DotName dotName, DomainObjectSet collector, IndexView index) {
        Collection<AnnotationInstance> jpaAnnotations = index.getAnnotations(dotName);
        if (jpaAnnotations != null && jpaAnnotations.size() > 0) {
            for (AnnotationInstance annotation : jpaAnnotations) {
                DotName targetDotName = annotation.target().asClass().name();
                addClassHierarchyToReflectiveList(collector, index, targetDotName);
                collector.addEntity(targetDotName.toString());
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
    private static void addClassHierarchyToReflectiveList(DomainObjectSet collector, IndexView index, DotName className) {
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
            DotName type = fieldInfo.type().name();
            ClassInfo typeCi = index.getClassByName(type);
            if (typeCi != null && typeCi.superName().equals(ENUM)) {
                collector.addEnumType(type.toString());
            }
        }

        //Capture this one (for various needs: Reflective access enablement, Hibernate enhancement, JPA Template)
        collector.addEntity(className.toString());
        // add superclass recursively
        addClassHierarchyToReflectiveList(collector, index, classInfo.superName());
        // add interfaces recursively
        for (DotName interfaceDotName : classInfo.interfaceNames()) {
            addClassHierarchyToReflectiveList(collector, index, interfaceDotName);
        }
    }

    private static class DomainObjectSet implements KnownDomainObjects {

        private final Set<String> classNames = new HashSet<String>();
        private final Set<String> enumTypes = new HashSet<String>();

        public void addEntity(final String className) {
            classNames.add(className);
        }

        void dumpAllToJPATemplate(final JPADeploymentTemplate template) {
            for (String className : classNames) {
                template.addEntity(className);
            }
        }


        void registerAllForReflection(final BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
            for (String className : classNames) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, className));
            }
            if (!enumTypes.isEmpty()) {
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, Enum.class.getName()));
                for (String className : enumTypes) {
                    reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, className));
                }
            }
        }

        @Override
        public boolean contains(final String className) {
            return classNames.contains(className);
        }

        @Override
        public Set<String> getClassNames() {
            return classNames;
        }

        public void addEnumType(String s) {
            enumTypes.add(s);
        }
    }

}
