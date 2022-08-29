package io.quarkus.hibernate.orm.deployment;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.hibernate.orm.runtime.HibernateOrmDisabledRecorder;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;

@BuildSteps(onlyIfNot = HibernateOrmEnabled.class)
class HibernateOrmDisabledProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void disableHibernateOrm(HibernateOrmDisabledRecorder recorder, HibernateOrmRuntimeConfig runtimeConfig,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors) {
        // The disabling itself is done through conditions on build steps (see uses of HibernateOrmEnabled.class)

        // We still want to check that nobody tries to set quarkus.hibernate-orm.active = true at runtime
        // if Hibernate ORM is disabled, though:
        Set<String> persistenceUnitNames = persistenceUnitDescriptors.stream()
                .map(PersistenceUnitDescriptorBuildItem::getConfigurationName)
                .collect(Collectors.toSet());
        recorder.checkNoExplicitActiveTrue(runtimeConfig, persistenceUnitNames);
    }

}
