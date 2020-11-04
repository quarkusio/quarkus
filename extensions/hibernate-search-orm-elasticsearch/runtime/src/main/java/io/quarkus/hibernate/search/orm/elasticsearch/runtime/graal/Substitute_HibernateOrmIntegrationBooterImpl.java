package io.quarkus.hibernate.search.orm.elasticsearch.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Force two phase-boot so that bootstrap code can be DCEd.
 */
@TargetClass(className = "org.hibernate.search.mapper.orm.bootstrap.impl.HibernateOrmIntegrationBooterImpl")
final class Substitute_HibernateOrmIntegrationBooterImpl {

    @Substitute
    private HibernateOrmIntegrationPartialBuildState doBootFirstPhase() {
        throw new IllegalStateException("Partial build state should have been generated during the static init phase.");
    }

    @TargetClass(className = "org.hibernate.search.mapper.orm.bootstrap.impl.HibernateOrmIntegrationBooterImpl", innerClass = "HibernateOrmIntegrationPartialBuildState")
    final static class HibernateOrmIntegrationPartialBuildState {

    }
}
