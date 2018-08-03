package org.jboss.shamrock.jpa;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;

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

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {
        // Hibernate specific reflective classes
        processorContext.addReflectiveClass(false, false, org.hibernate.jpa.HibernatePersistenceProvider.class.getName());
        processorContext.addReflectiveClass(false, false, org.hibernate.persister.entity.SingleTableEntityPersister.class.getName());
        // TODO add reflectconfig.json


        // list all entities and create a JPADeploymentTemplate out of it
        // Not functional as we will need one deployment template per persistence unit
        final IndexView index = archiveContext.getIndex();
        Collection<AnnotationInstance> jpaAnnotations = index.getAnnotations(JPA_ENTITY);
        if (jpaAnnotations != null && jpaAnnotations.size() > 0) {
            // TODO priority?
            try (BytecodeRecorder context = processorContext.addStaticInitTask(100)) {
                JPADeploymentTemplate template = context.getRecordingProxy(JPADeploymentTemplate.class);
                for (AnnotationInstance annotation : jpaAnnotations) {
                    String entityClass = annotation.target().asClass().toString();
                    System.out.println(entityClass);
                    processorContext.addReflectiveClass(true, true, entityClass);
                    template.addEntity(annotation.target().asClass().toString());
                }
                template.enlistPersistenceUnit();
            }
        }

    }

    @Override
    public int getPriority() {
        // Because we are the best
        return 1;
    }
}
