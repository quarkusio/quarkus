package io.quarkus.hibernate.orm.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.hibernate.orm.runtime.HibernateOrmDisabledRecorder;

@BuildSteps(onlyIfNot = HibernateOrmEnabled.class)
class HibernateOrmDisabledProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void disableHibernateOrm(HibernateOrmDisabledRecorder recorder) {
        // The disabling itself is done through conditions on build steps (see uses of HibernateOrmEnabled.class)

        // We still want to check that nobody tries to set quarkus.hibernate-orm.active = true at runtime
        // if Hibernate ORM is disabled, though:
        recorder.checkNoExplicitActiveTrue();
    }
}
