package io.quarkus.hibernate.search.orm.elasticsearch.runtime.graal;

import org.hibernate.search.mapper.orm.bootstrap.spi.HibernateOrmIntegrationBooterBehavior;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Disallow the first phase of boot during runtime init so that bootstrap code can be DCEd.
 */
@TargetClass(className = "org.hibernate.search.mapper.orm.bootstrap.spi.HibernateOrmIntegrationBooterBehavior")
final class Substitute_HibernateOrmIntegrationBooterBehavior {

    @Substitute
    public static <T> T bootFirstPhase(HibernateOrmIntegrationBooterBehavior.BootPhase<T> phase) {
        throw new IllegalStateException("The first phase of Hibernate Search's boot should have occurred during static init.");
    }

}
