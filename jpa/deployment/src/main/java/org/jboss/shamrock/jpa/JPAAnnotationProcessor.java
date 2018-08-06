package org.jboss.shamrock.jpa;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
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
                String jpaClassName = null;
                switch (target.kind()) {
                    case FIELD:
                        // TODO could fail if that's an array or a generic type
                        jpaClassName = target.asField().type().toString();
                        break;
                    case METHOD:
                        // TODO could fail if that's an array or a generic type
                        jpaClassName = target.asMethod().returnType().toString();
                        break;
                    default:
                        throw new IllegalStateException("[internal error] @Embedded placed on a unknown element: " + target);
                }
                processorContext.addReflectiveClass(true, true, jpaClassName);
            }
        }
    }

    private void enlistJPAModelClasses(DotName dotName, ProcessorContext processorContext, JPADeploymentTemplate template, IndexView index) {
        Collection<AnnotationInstance> jpaAnnotations = index.getAnnotations(dotName);
        if (jpaAnnotations != null && jpaAnnotations.size() > 0) {
            for (AnnotationInstance annotation : jpaAnnotations) {
                String entityClass = annotation.target().asClass().toString();
                processorContext.addReflectiveClass(true, true, entityClass);
                template.addEntity(annotation.target().asClass().toString());
            }
        }
    }

    @Override
    public int getPriority() {
        // Because we are the best
        return 1;
    }
}
