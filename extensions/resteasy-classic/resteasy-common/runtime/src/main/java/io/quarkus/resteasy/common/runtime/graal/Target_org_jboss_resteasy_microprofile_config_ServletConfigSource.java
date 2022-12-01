package io.quarkus.resteasy.common.runtime.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.jboss.resteasy.microprofile.config.ServletConfigSource", onlyWith = {
        ServletMissing.class
})
final class Target_org_jboss_resteasy_microprofile_config_ServletConfigSource {
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private static Class<?> clazz;
}
