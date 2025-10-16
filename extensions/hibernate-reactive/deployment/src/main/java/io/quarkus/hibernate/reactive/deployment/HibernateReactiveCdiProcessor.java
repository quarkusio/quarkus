package io.quarkus.hibernate.reactive.deployment;

import static io.quarkus.hibernate.reactive.deployment.ClassNames.IMPLEMENTOR;
import static io.quarkus.hibernate.reactive.deployment.ClassNames.MUTINY_SESSION;
import static io.quarkus.hibernate.reactive.deployment.ClassNames.MUTINY_SESSION_FACTORY;
import static io.quarkus.hibernate.reactive.deployment.ClassNames.MUTINY_STATELESS_SESSION;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;

import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;

import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.runtime.JPAConfig;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.reactive.runtime.HibernateReactiveRecorder;

public class HibernateReactiveCdiProcessor {

    private static final List<DotName> MUTINY_SESSION_FACTORY_EXPOSED_TYPES = Arrays.asList(
            MUTINY_SESSION_FACTORY, IMPLEMENTOR);

    private static final List<DotName> MUTINY_SESSION_EXPOSED_TYPES = Arrays.asList(
            MUTINY_SESSION);

    private static final List<DotName> MUTINY_STATELESS_SESSION_EXPOSED_TYPES = Arrays.asList(
            MUTINY_STATELESS_SESSION);

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void registerBeans(
            HibernateReactiveRecorder recorder,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {
        if (persistenceUnitDescriptors.isEmpty()) {
            // No persistence units have been configured so bail out
            return;
        }

        for (PersistenceUnitDescriptorBuildItem persistenceUnitDescriptor : persistenceUnitDescriptors) {
            String persistenceUnitName = persistenceUnitDescriptor.getPersistenceUnitName();
            boolean isDefaultPU = PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName);
            boolean isReactive = persistenceUnitDescriptor.isReactive();

            if (isReactive) {
                PersistenceUnitReference puRef = new PersistenceUnitReference(
                        persistenceUnitName,
                        recorder.checkActiveSupplier(
                                persistenceUnitDescriptor.getPersistenceUnitName(),
                                persistenceUnitDescriptor.getConfig().getDataSource(),
                                persistenceUnitDescriptor.getConfig().getEntityClassNames()));

                produceSessionFactoryBean(syntheticBeanBuildItemBuildProducer, recorder, puRef);

                produceSessionBeans(syntheticBeanBuildItemBuildProducer, recorder, puRef);
            }

        }
    }

    private void produceSessionFactoryBean(
            BuildProducer<SyntheticBeanBuildItem> producer,
            HibernateReactiveRecorder recorder,
            PersistenceUnitReference puRef) {

        producer.produce(createSyntheticBean(puRef,
                Mutiny.SessionFactory.class, MUTINY_SESSION_FACTORY_EXPOSED_TYPES, true)
                .createWith(recorder.mutinySessionFactory(puRef.persistenceUnitName))
                .addInjectionPoint(ClassType.create(DotName.createSimple(JPAConfig.class)))
                .done());
    }

    private void produceSessionBeans(
            BuildProducer<SyntheticBeanBuildItem> producer,
            HibernateReactiveRecorder recorder,
            PersistenceUnitReference puRef) {

        // Create Session bean
        producer.produce(createSyntheticBean(puRef,
                Mutiny.Session.class, MUTINY_SESSION_EXPOSED_TYPES, false)
                .createWith(recorder.sessionSupplier(puRef.persistenceUnitName))
                .done());

        // Create StatelessSession bean
        producer.produce(createSyntheticBean(puRef,
                Mutiny.StatelessSession.class, MUTINY_STATELESS_SESSION_EXPOSED_TYPES, false)
                .createWith(recorder.statelessSessionSupplier(puRef.persistenceUnitName))
                .done());
    }

    private static <T> SyntheticBeanBuildItem.ExtendedBeanConfigurator createSyntheticBean(PersistenceUnitReference puRef,
            Class<T> type, List<DotName> allExposedTypes, boolean defaultBean) {
        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(type)
                // NOTE: this is using ApplicationScope and not Singleton, by design, in order to be mockable
                // See https://github.com/quarkusio/quarkus/issues/16437
                .scope(ApplicationScoped.class)
                .unremovable()
                .setRuntimeInit()
                // Note persistence units _actually_ get started a bit earlier, each in its own thread. See JPAConfig#startAll.
                // This startup() call is only necessary in order to trigger Arc's usage checks (fail startup if bean injected when a PU is inactive).
                .startup()
                .checkActive(puRef.checkActiveSupplier);

        for (DotName exposedType : allExposedTypes) {
            configurator.addType(exposedType);
        }

        if (defaultBean) {
            configurator.defaultBean();
        }

        boolean defaultPuName = PersistenceUnitUtil.isDefaultPersistenceUnit(puRef.persistenceUnitName);
        if (defaultPuName) {
            configurator.addQualifier(Default.class);
        }

        configurator.addQualifier().annotation(DotNames.NAMED).addValue("value", puRef.persistenceUnitName).done();
        configurator.addQualifier().annotation(PersistenceUnit.class).addValue("value", puRef.persistenceUnitName).done();

        return configurator;
    }

    private record PersistenceUnitReference(
            String persistenceUnitName,
            Supplier<ActiveResult> checkActiveSupplier) {
    }

}
