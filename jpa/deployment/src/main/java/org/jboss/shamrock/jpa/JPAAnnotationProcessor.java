package org.jboss.shamrock.jpa;

import org.jboss.jandex.*;
import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import java.util.Collection;

/**
 * Simulacrum of JPA bootstrap.
 * <p>
 * This does not address the proper integration with Hibernate
 * Rather prepare the path to providing the right metadata
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class JPAAnnotationProcessor implements ResourceProcessor {

    private static final DotName JPA_ENTITY = DotName.createSimple(Entity.class.getName());
    private static final DotName EMBEDDABLE = DotName.createSimple(Embeddable.class.getName());
    private static final DotName EMBEDDED = DotName.createSimple(Embedded.class.getName());

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {
        // Hibernate specific reflective classes
        processorContext.addReflectiveClass(false, false, org.hibernate.jpa.HibernatePersistenceProvider.class.getName());
        processorContext.addReflectiveClass(false, false, org.hibernate.persister.entity.SingleTableEntityPersister.class.getName());
        // TODO add reflectconfig.json


        // list all entities and create a JPADeploymentTemplate out of it
        // Not functional as we will need one deployment template per persistence unit
        final IndexView index = archiveContext.getIndex();
        // TODO what priority to give JPA?
        try (BytecodeRecorder context = processorContext.addStaticInitTask(100)) {
            JPADeploymentTemplate template = context.getRecordingProxy(JPADeploymentTemplate.class);
            enlistJPAModelClasses(JPA_ENTITY, processorContext, template, index);
            enlistJPAModelClasses(EMBEDDABLE, processorContext, template, index);
            enlistReturnType(processorContext, index);

            template.enlistPersistenceUnit();
        }
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

    private void enlistJPAModelClasses(DotName dotName, ProcessorContext processorContext, JPADeploymentTemplate template, IndexView index) {
        Collection<AnnotationInstance> jpaAnnotations = index.getAnnotations(dotName);
        if (jpaAnnotations != null && jpaAnnotations.size() > 0) {
            for (AnnotationInstance annotation : jpaAnnotations) {
                DotName targetDotName = annotation.target().asClass().name();
                addClassHierarchyToReflectiveList(processorContext, index, targetDotName);
                template.addEntity(targetDotName.toString());
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

    @Override
    public int getPriority() {
        // Because we are the best
        return 1;
    }
}
