package io.quarkus.hibernate.reactive.deployment;

import static io.quarkus.hibernate.reactive.deployment.ClassNames.IMPLEMENTOR;
import static io.quarkus.hibernate.reactive.deployment.ClassNames.MUTINY_SESSION_FACTORY;

import java.util.Arrays;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;

import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.hibernate.orm.deployment.ClassNames;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.runtime.JPAConfig;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.reactive.runtime.HibernateReactiveRecorder;

public class HibernateReactiveCdiProcessor {

    private static final List<DotName> MUTINY_SESSION_FACTORY_EXPOSED_TYPES = Arrays.asList(
            MUTINY_SESSION_FACTORY, IMPLEMENTOR);

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void produceSessionFactoryBean(
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
                SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                        .configure(Mutiny.SessionFactory.class)
                        // NOTE: this is using ApplicationScope and not Singleton, by design, in order to be mockable
                        // See https://github.com/quarkusio/quarkus/issues/16437
                        .scope(ApplicationScoped.class)
                        .unremovable()
                        .setRuntimeInit()
                        // Note persistence units _actually_ get started a bit earlier, each in its own thread. See JPAConfig#startAll.
                        // This startup() call is only necessary in order to trigger Arc's usage checks (fail startup if bean injected when a PU is inactive).
                        .startup()
                        .checkActive(recorder.checkActiveSupplier(persistenceUnitName,
                                persistenceUnitDescriptor.getConfig().getDataSource(),
                                persistenceUnitDescriptor.getConfig().getEntityClassNames()))
                        .createWith(recorder.mutinySessionFactory(persistenceUnitName))
                        .addInjectionPoint(ClassType.create(DotName.createSimple(JPAConfig.class)));

                for (DotName exposedType : MUTINY_SESSION_FACTORY_EXPOSED_TYPES) {
                    configurator.addType(exposedType);
                }

                configurator.defaultBean();

                if (isDefaultPU) {
                    configurator.addQualifier(Default.class);
                } else {
                    configurator.addQualifier().annotation(ClassNames.QUARKUS_PERSISTENCE_UNIT)
                            .addValue("value", persistenceUnitName).done();
                }

                syntheticBeanBuildItemBuildProducer.produce(configurator.done());
            }

        }
    }

}
