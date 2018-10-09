package org.jboss.shamrock.jpa.cdi;

import java.util.List;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.BeanDeployment;
import org.jboss.shamrock.deployment.Capabilities;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.RuntimePriority;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;
import org.jboss.shamrock.jpa.HibernateResourceProcessor;
import org.jboss.shamrock.jpa.runtime.DefaultEntityManagerFactoryProducer;
import org.jboss.shamrock.jpa.runtime.DefaultEntityManagerProducer;
import org.jboss.shamrock.jpa.runtime.JPAConfig;
import org.jboss.shamrock.jpa.runtime.JPADeploymentTemplate;
import org.jboss.shamrock.jpa.runtime.TransactionEntityManagers;

public class HibernateCdiResourceProcessor implements ResourceProcessor {

    private static final DotName PERSISTENCE_CONTEXT = DotName.createSimple(PersistenceContext.class.getName());
    private static final DotName PERSISTENCE_UNIT = DotName.createSimple(PersistenceUnit.class.getName());
    private static final DotName PRODUCES = DotName.createSimple(Produces.class.getName());

    @Inject
    BeanDeployment beanDeployment;

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {

        try (BytecodeRecorder recorder = processorContext.addDeploymentTask(RuntimePriority.BOOTSTRAP_EMF)) {
            JPADeploymentTemplate template = recorder.getRecordingProxy(JPADeploymentTemplate.class);

            beanDeployment.addAdditionalBean(JPAConfig.class, TransactionEntityManagers.class);

            if (processorContext.isCapabilityPresent(Capabilities.CDI_ARC)) {
                processorContext.createResource("META-INF/services/org.jboss.protean.arc.ResourceReferenceProvider",
                        "org.jboss.shamrock.jpa.runtime.JPAResourceReferenceProvider".getBytes());
                beanDeployment.addResourceAnnotation(PERSISTENCE_CONTEXT);
                beanDeployment.addResourceAnnotation(PERSISTENCE_UNIT);
            }

            template.initializeJpa(null, processorContext.isCapabilityPresent(Capabilities.TRANSACTIONS));

            // Bootstrap all persistence units
            List<PersistenceUnitDescriptor> pus = processorContext
                    .getProperty(HibernateResourceProcessor.PARSED_DESCRIPTORS);
            for (PersistenceUnitDescriptor persistenceUnitDescriptor : pus) {
                template.bootstrapPersistenceUnit(null, persistenceUnitDescriptor.getName());
            }
            template.initDefaultPersistenceUnit(null);

            if (pus.size() == 1) {
                // There is only one persistence unit - register CDI beans for EM and EMF if no
                // producers are defined
                if (isUserDefinedProducerMissing(archiveContext.getCombinedIndex(), PERSISTENCE_UNIT)) {
                    beanDeployment.addAdditionalBean(DefaultEntityManagerFactoryProducer.class);
                }
                if (isUserDefinedProducerMissing(archiveContext.getCombinedIndex(), PERSISTENCE_CONTEXT)) {
                    beanDeployment.addAdditionalBean(DefaultEntityManagerProducer.class);
                }
            }
        }
    }

    private boolean isUserDefinedProducerMissing(IndexView index, DotName annotationName) {
        for (AnnotationInstance annotationInstance : index.getAnnotations(annotationName)) {
            if (annotationInstance.target().kind() == AnnotationTarget.Kind.METHOD) {
                if (annotationInstance.target().asMethod().hasAnnotation(PRODUCES)) {
                    return false;
                }
            } else if (annotationInstance.target().kind() == AnnotationTarget.Kind.FIELD) {
                for (AnnotationInstance i : annotationInstance.target().asField().annotations()) {
                    if (i.name().equals(PRODUCES)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public int getPriority() {
        return 100;
    }

}
