package org.jboss.shamrock.jpa;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;

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

    private final ArchiveContext archiveContext;
    private final ProcessorContext processorContext;

    JpaJandexScavenger(final ArchiveContext archiveContext, final ProcessorContext processorContext) {
        this.archiveContext = archiveContext;
        this.processorContext = processorContext;
    }

    public KnownDomainObjects discoverModelAndRegisterForReflection() throws IOException {
        // list all entities and create a JPADeploymentTemplate out of it
        // Not functional as we will need one deployment template per persistence unit
        final IndexView index = archiveContext.getCombinedIndex();
        final DomainObjectSet collector;
        // TODO what priority to give JPA?
        try (BytecodeRecorder context = processorContext.addStaticInitTask(100)) {
            JPADeploymentTemplate template = context.getRecordingProxy(JPADeploymentTemplate.class);
            collector = new DomainObjectSet(template);
            enlistJPAModelClasses(JPA_ENTITY, processorContext, collector, index);
            enlistJPAModelClasses(EMBEDDABLE, processorContext, collector, index);
            enlistReturnType(processorContext, index);
            template.enlistPersistenceUnit();
        }
        collector.clearLinkToTemplate();//no longer need the JPADeploymentTemplate
        return collector;
    }

    private void enlistReturnType(ProcessorContext processorContext, IndexView index) {
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
                addClassHierarchyToReflectiveList(processorContext, index, jpaClassName);
            }
        }
    }

    private void enlistJPAModelClasses(DotName dotName, ProcessorContext processorContext, DomainObjectSet collector, IndexView index) {
        Collection<AnnotationInstance> jpaAnnotations = index.getAnnotations(dotName);
        if (jpaAnnotations != null && jpaAnnotations.size() > 0) {
            for (AnnotationInstance annotation : jpaAnnotations) {
                DotName targetDotName = annotation.target().asClass().name();
                addClassHierarchyToReflectiveList(processorContext, index, targetDotName);
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
    private void addClassHierarchyToReflectiveList(ProcessorContext processorContext, IndexView index, DotName className) {
        // If type is not Object
        // recursively add superclass and interfaces
        if (className == null) {
            // java.lang.Object
            return;
        }
        ClassInfo classInfo = index.getClassByName(className);
        if (classInfo == null) {
            if (className == ClassType.OBJECT_TYPE.name()) {
                return;
            }
            else {
                throw new IllegalStateException("The Jandex index is not complete, missing: " + className.toString());
            }
        }
        // add class for reflection
        processorContext.addReflectiveClass(true, true, className.toString());
        // add superclass recursively
        addClassHierarchyToReflectiveList(processorContext, index, classInfo.superName());
        // add interfaces recursively
        for (DotName interfaceDotName : classInfo.interfaceNames()) {
            addClassHierarchyToReflectiveList(processorContext, index, interfaceDotName);
        }
    }

    private static class DomainObjectSet implements KnownDomainObjects {

        private JPADeploymentTemplate template;
        private final Set<String> classNames = new HashSet<String>();

        public DomainObjectSet(final JPADeploymentTemplate template) {
            this.template = template;
        }

        public void addEntity(final String name) {
            classNames.add(name);
            template.addEntity(name);
        }

        void clearLinkToTemplate(){
            this.template = null;
        }

        @Override
        public boolean contains(final String className) {
            return classNames.contains(className);
        }
    }

}
