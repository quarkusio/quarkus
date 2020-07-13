package io.quarkus.smallrye.opentracing.runtime.graal;

import org.jboss.resteasy.microprofile.config.ServletConfigSource;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * see {@link Target_org_jboss_resteasy_microprofile_config_ServletContextConfigSource}
 */
@TargetClass(value = ServletConfigSource.class, onlyWith = UndertowMissing.class)
final class Target_org_jboss_resteasy_microprofile_config_ServletConfigSource {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static boolean SERVLET_AVAILABLE = false;

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private static Class<?> clazz;
}
