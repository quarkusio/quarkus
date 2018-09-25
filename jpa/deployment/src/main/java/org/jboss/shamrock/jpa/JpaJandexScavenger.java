package org.jboss.shamrock.jpa;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Modifier;
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
import org.hibernate.protean.impl.PersistenceUnitsHolder;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;
import org.jboss.shamrock.jpa.runtime.JPADeploymentTemplate;

/**
 * Scan the Jandex index to find JPA entities (and embeddables supporting entity models).
 *
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

    private final ArchiveContext archiveContext;
    private final ProcessorContext processorContext;
    private final List<ParsedPersistenceXmlDescriptor> descriptors;

    JpaJandexScavenger(final ArchiveContext archiveContext, final ProcessorContext processorContext, List<ParsedPersistenceXmlDescriptor> descriptors) {
        this.archiveContext = archiveContext;
        this.processorContext = processorContext;
        this.descriptors = descriptors;
    }

    public KnownDomainObjects discoverModelAndRegisterForReflection() throws IOException {
        // list all entities and create a JPADeploymentTemplate out of it
        // Not functional as we will need one deployment template per persistence unit
        final IndexView index = archiveContext.getCombinedIndex();
        final DomainObjectSet collector = new DomainObjectSet();

        enlistJPAModelClasses(JPA_ENTITY, collector, index);
        enlistJPAModelClasses(EMBEDDABLE, collector, index);
        enlistJPAModelClasses(MAPPED_SUPERCLASS, collector, index);
        enlistReturnType(collector, index);

        for (PersistenceUnitDescriptor pud : descriptors) {
            enlistExplicitClasses(pud.getManagedClassNames(), collector, index);
        }

        collector.registerAllForReflection(processorContext);

        // TODO what priority to give JPA?
        try (BytecodeRecorder context = processorContext.addStaticInitTask(100)) {
            JPADeploymentTemplate template = context.getRecordingProxy(JPADeploymentTemplate.class);
            collector.dumpAllToJPATemplate(template);
            template.enlistPersistenceUnit();
            template.callHibernateFeatureInit();
        }


        return collector;
    }

    private static void enlistExplicitClasses(List<String> managedClassNames, DomainObjectSet collector, IndexView index) {
        for (String className : managedClassNames) {
            DotName dotName = DotName.createSimple(className);
            boolean isInIndex = index.getClassByName(dotName) != null;
            if (isInIndex) {
                addClassHierarchyToReflectiveList(collector, index, dotName);
            }
            else {
                // We do lipstick service by manually adding explicitly the <class> reference but not navigating the hierarchy
                System.out.println("[WARNING] Did not find `" + className + "` in the indexed jars. You likely forgot to add shamrock-build.yaml in your dependency jar. See https://github.com/protean-project/shamrock/#indexing-and-application-classes for more info.");
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
     *
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
            if (className == ClassType.OBJECT_TYPE.name() || className.toString().equals(Serializable.class.getName())) {
                return;
            }
            else {
                throw new IllegalStateException("The Jandex index is not complete, missing: " + className.toString());
            }
        }
        //we need to check for enums
        for(FieldInfo fieldInfo : classInfo.fields()) {
            DotName type = fieldInfo.type().name();
            ClassInfo typeCi = index.getClassByName(type);
            if(typeCi != null && typeCi.superName().equals(ENUM)) {
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



        void registerAllForReflection(final ProcessorContext processorContext) {
            for (String className : classNames) {
                processorContext.addReflectiveClass(true, true, className);
            }
            if(!enumTypes.isEmpty()) {
                processorContext.addReflectiveClass(true, false, Enum.class.getName());
                for (String className : enumTypes) {
                    processorContext.addReflectiveClass(true, false, className);
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
