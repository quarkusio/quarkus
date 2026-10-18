package io.quarkus.hibernate.search.standalone.elasticsearch.runtime.graal;

import org.hibernate.search.mapper.pojo.standalone.bootstrap.spi.StandalonePojoIntegrationBooterBehavior;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Disallow the first phase of boot during runtime init so that bootstrap code can be DCEd.
 */
@TargetClass(className = "org.hibernate.search.mapper.pojo.standalone.bootstrap.spi.StandalonePojoIntegrationBooterBehavior")
final class Substitute_StandalonePojoIntegrationBooterBehavior {

    @Substitute
    public static <T> T bootFirstPhase(StandalonePojoIntegrationBooterBehavior.BootPhase<T> phase) {
        throw new IllegalStateException("The first phase of Hibernate Search's boot should have occurred during static init.");
    }

}
