package io.quarkus.hibernate.search.standalone.elasticsearch.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.HibernateSearchStandaloneRecorder;

@BuildSteps(onlyIfNot = HibernateSearchStandaloneEnabled.class)
class HibernateSearchStandaloneDisabledProcessor {
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void disableHibernateSearch(HibernateSearchStandaloneRecorder recorder) {
        recorder.checkNoExplicitActiveTrue();
        recorder.clearPreBootState();
    }
}
