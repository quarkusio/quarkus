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
            String persistenceUnitConfigName = persistenceUnitDescriptor.getConfigurationName();
            boolean isDefaultPU = PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitConfigName);
            boolean isReactive = persistenceUnitDescriptor.isReactive();

            if (isReactive) {
                SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                        .configure(Mutiny.SessionFactory.class)
                        // NOTE: this is using ApplicationScope and not Singleton, by design, in order to be mockable
                        // See https://github.com/quarkusio/quarkus/issues/16437
                        .scope(ApplicationScoped.class)
                        .unremovable()
                        .setRuntimeInit();

                for (DotName exposedType : MUTINY_SESSION_FACTORY_EXPOSED_TYPES) {
                    configurator.addType(exposedType);
                }

                configurator.defaultBean();

                if (isDefaultPU) {
                    configurator.addQualifier(Default.class);
                }

                syntheticBeanBuildItemBuildProducer
                        .produce(configurator
                                .createWith(recorder.mutinySessionFactory(persistenceUnitName))
                                .addInjectionPoint(ClassType.create(DotName.createSimple(JPAConfig.class)))
                                .done());
            }

        }
    }

}
