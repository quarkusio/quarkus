package io.quarkus.infinispan.client.substitutions;

import org.infinispan.client.hotrod.configuration.StatisticsConfigurationBuilder;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Don't allow the user to enable jmx in substrate
 * 
 * @author William Burns
 */
@TargetClass(StatisticsConfigurationBuilder.class)
public final class SubstituteStatisticsConfigurationBuilder {
    @Substitute
    public StatisticsConfigurationBuilder jmxEnabled(boolean enabled) {
        throw new UnsupportedOperationException("JMX is not available in Substrate");
    }
}
