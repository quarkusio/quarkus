package io.quarkus.infinispan.client.runtime.graal;

import org.infinispan.client.hotrod.configuration.StatisticsConfiguration;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * JMX is always disabled in native mode
 * 
 * @author William Burns
 */
@TargetClass(StatisticsConfiguration.class)
public final class SubstituteStatisticsConfiguration {
    @Substitute
    public boolean jmxEnabled() {
        return false;
    }
}
