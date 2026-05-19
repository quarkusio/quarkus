package io.quarkus.hibernate.orm.runtime.graal;

import com.github.benmanes.caffeine.jcache.CacheProxy;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Removes some caffeine-jcache features related to JMX,
 * because we don't want to put in the work to make that work in native mode.
 */
// TODO should this be in quarkus-caffeine, with caffeine-jcache as a "provided" dependency?
@TargetClass(CacheProxy.class)
final class Substitute_CacheProxy {
    @Substitute
    void enableManagement(boolean enabled) {
        if (enabled) {
            throw new IllegalStateException("JMX is not available in native images.");
        }
    }

    @Substitute
    void enableStatistics(boolean enabled) {
        if (enabled) {
            throw new IllegalStateException("JMX is not available in native images.");
        }
    }
}
