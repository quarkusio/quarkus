package org.jboss.shamrock.jpa.cdi;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.ClassOutput;
import org.jboss.protean.gizmo.FieldCreator;
import org.jboss.protean.gizmo.FieldDescriptor;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.BeanArchiveIndex;
import org.jboss.shamrock.deployment.BeanDeployment;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.RuntimePriority;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;
import org.jboss.shamrock.jpa.runtime.JPADeploymentTemplate;
import org.jboss.shamrock.jpa.runtime.cdi.SystemEntityManager;
import org.jboss.shamrock.jpa.runtime.cdi.TransactionScopedEntityManager;

public class HibernateCdiResourceProcessor implements ResourceProcessor {

    private static final DotName PERSISTENCE_CONTEXT = DotName.createSimple(PersistenceContext.class.getName());
    private static final DotName PERSISTENCE_UNIT = DotName.createSimple(PersistenceUnit.class.getName());
    private static final DotName PRODUCES = DotName.createSimple(Produces.class.getName());

    @Inject
    private BeanDeployment beanDeployment;

    @Inject
    BeanArchiveIndex beanArchiveIndex;

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {

        Set<String> knownUnitNames = new HashSet<>();
        Set<String> knownContextNames = new HashSet<>();
        scanForAnnotations(archiveContext, knownUnitNames, PERSISTENCE_UNIT);
        scanForAnnotations(archiveContext, knownContextNames, PERSISTENCE_CONTEXT);
        knownUnitNames.remove(""); //TODO: support for the default PU
        //now create producer beans for all of the above unit names
        //this is not great, we really need a better way to do this than generating bytecode


        Set<String> allKnownNames = new HashSet<>(knownUnitNames);
        allKnownNames.addAll(knownContextNames);

        for (String name : allKnownNames) {
            String className = getClass().getName() + "$$EMFProducer-" + name;
            AtomicReference<byte[]> bytes = new AtomicReference<>();
            try (ClassCreator creator = new ClassCreator(new InMemoryClassOutput(bytes, processorContext), className, null, Object.class.getName())) {

                creator.addAnnotation(Dependent.class);
                MethodCreator producer = creator.getMethodCreator("producerMethod", EntityManagerFactory.class);
                producer.addAnnotation(Produces.class);
                producer.addAnnotation(ApplicationScoped.class);
                if (!knownUnitNames.contains(name)) {
                    //there was no @PersistenceUnit producer with this name
                    //this means that we still need it, but the user would not be expecting a bean to be registered
                    //we register an artificial qualifier that we will use for the managed persistence contexts
                    producer.addAnnotation(SystemEntityManager.class);
                }

                ResultHandle ret = producer.invokeStaticMethod(MethodDescriptor.ofMethod(Persistence.class, "createEntityManagerFactory", EntityManagerFactory.class, String.class), producer.load(name));
                producer.returnValue(ret);
            }
            beanDeployment.addGeneratedBean(className, bytes.get());
        }


        try(BytecodeRecorder recorder = processorContext.addDeploymentTask(RuntimePriority.BOOTSTRAP_EMF)) {
            JPADeploymentTemplate template = recorder.getRecordingProxy(JPADeploymentTemplate.class);

            for (String name : knownContextNames) {
                String className = getClass().getName() + "$$EMProducer-" + name;
                AtomicReference<byte[]> bytes = new AtomicReference<>();

                //we need to know if transactions are present or not
                //TODO: this should be based on if a PU is JTA enabled or not
                if (processorContext.isCapabilityPresent("transactions")) {
                    boolean system = false;
                    try (ClassCreator creator = new ClassCreator(new InMemoryClassOutput(bytes, processorContext), className, null, Object.class.getName())) {

                        creator.addAnnotation(Dependent.class);

                        FieldCreator emfField = creator.getFieldCreator("emf", EntityManagerFactory.class);
                        emfField.addAnnotation(Inject.class);
                        if (!knownUnitNames.contains(name)) {
                            emfField.addAnnotation(SystemEntityManager.class);
                            system = true;
                        }
                        FieldDescriptor emf = emfField.getFieldDescriptor();


                        FieldCreator tsrField = creator.getFieldCreator("tsr", TransactionSynchronizationRegistry.class);
                        tsrField.addAnnotation(Inject.class);
                        FieldDescriptor tsr = tsrField.getFieldDescriptor();


                        FieldCreator tmField = creator.getFieldCreator("tm", TransactionManager.class);
                        tmField.addAnnotation(Inject.class);
                        FieldDescriptor tm = tmField.getFieldDescriptor();

                        MethodCreator producer = creator.getMethodCreator("producerMethod", EntityManager.class);
                        producer.addAnnotation(Produces.class);
                        producer.addAnnotation(RequestScoped.class);

                        ResultHandle emfRh = producer.readInstanceField(emf, producer.getThis());
                        ResultHandle tsrRh = producer.readInstanceField(tsr, producer.getThis());
                        ResultHandle tmRh = producer.readInstanceField(tm, producer.getThis());

                        producer.returnValue(producer.newInstance(MethodDescriptor.ofConstructor(TransactionScopedEntityManager.class, TransactionManager.class, TransactionSynchronizationRegistry.class, EntityManagerFactory.class), tmRh, tsrRh, emfRh));


                        MethodCreator disposer = creator.getMethodCreator("disposerMethod", void.class, EntityManager.class);
                        disposer.getParameterAnnotations(0).addAnnotation(Disposes.class);
                        disposer.invokeVirtualMethod(MethodDescriptor.ofMethod(TransactionScopedEntityManager.class, "requestDone", void.class), disposer.getMethodParam(0));
                        disposer.returnValue(null);

                    }
                    beanDeployment.addGeneratedBean(className, bytes.get());
                    template.boostrapPu(null, system);
                } else {
                    boolean system = false;
                    //if there is no TX support then we just use a super simple approach, and produce a normal EM
                    try (ClassCreator creator = new ClassCreator(new InMemoryClassOutput(bytes, processorContext), className, null, Object.class.getName())) {

                        creator.addAnnotation(Dependent.class);

                        FieldCreator emfField = creator.getFieldCreator("emf", EntityManagerFactory.class);
                        emfField.addAnnotation(Inject.class);
                        if (!knownUnitNames.contains(name)) {
                            emfField.addAnnotation(SystemEntityManager.class);
                            system = true;
                        }
                        FieldDescriptor emf = emfField.getFieldDescriptor();


                        MethodCreator producer = creator.getMethodCreator("producerMethod", EntityManager.class);
                        producer.addAnnotation(Produces.class);
                        producer.addAnnotation(Dependent.class);

                        ResultHandle factory = producer.readInstanceField(emf, producer.getThis());
                        producer.returnValue(producer.invokeInterfaceMethod(MethodDescriptor.ofMethod(EntityManagerFactory.class, "createEntityManager", EntityManager.class), factory));


                        MethodCreator disposer = creator.getMethodCreator("disposerMethod", void.class, EntityManager.class);
                        disposer.getParameterAnnotations(0).addAnnotation(Disposes.class);
                        disposer.invokeInterfaceMethod(MethodDescriptor.ofMethod(EntityManager.class, "close", void.class), disposer.getMethodParam(0));
                        disposer.returnValue(null);

                    }
                    beanDeployment.addGeneratedBean(className, bytes.get());
                    template.boostrapPu(null, system);
                }
            }
        }

    }

    private void scanForAnnotations(ArchiveContext archiveContext, Set<String> knownUnitNames, DotName nm) {
        for (AnnotationInstance anno : archiveContext.getCombinedIndex().getAnnotations(nm)) {
            AnnotationValue unitName = anno.value("unitName");
            if(unitName == null) {
                continue;
            }
            if (anno.target().kind() == AnnotationTarget.Kind.METHOD) {
                if (anno.target().asMethod().hasAnnotation(PRODUCES)) {
                    knownUnitNames.add(unitName.asString());
                }
            } else if (anno.target().kind() == AnnotationTarget.Kind.FIELD) {
                for (AnnotationInstance i : anno.target().asField().annotations()) {
                    if (i.name().equals(PRODUCES)) {
                        knownUnitNames.add(unitName.asString());
                        break;
                    }
                }
            } else if (anno.target().kind() == AnnotationTarget.Kind.CLASS) {
                for (AnnotationInstance i : anno.target().asClass().classAnnotations()) {
                    if (i.name().equals(PRODUCES)) {
                        knownUnitNames.add(unitName.asString());
                        break;
                    }
                }
            }
        }
    }

    @Override
    public int getPriority() {
        return 100;
    }

    private static class InMemoryClassOutput implements ClassOutput {
        private final AtomicReference<byte[]> bytes;
        private final ProcessorContext processorContext;

        public InMemoryClassOutput(AtomicReference<byte[]> bytes, ProcessorContext processorContext) {
            this.bytes = bytes;
            this.processorContext = processorContext;
        }

        @Override
        public void write(String name, byte[] data) {
            try {
                bytes.set(data);
                processorContext.addGeneratedClass(true, name, data);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
