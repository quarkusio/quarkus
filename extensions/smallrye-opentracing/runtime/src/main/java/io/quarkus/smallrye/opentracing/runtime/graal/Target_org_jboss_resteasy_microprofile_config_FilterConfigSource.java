package io.quarkus.smallrye.opentracing.runtime.graal;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * see {@link Target_org_jboss_resteasy_microprofile_config_ServletContextConfigSource}
 */
@TargetClass(className = "org.jboss.resteasy.microprofile.config.FilterConfigSource", onlyWith = {
        UndertowMissing.class,
        Target_org_jboss_resteasy_microprofile_config_FilterConfigSource.FilterConfigSourceIsLoaded.class
})
final class Target_org_jboss_resteasy_microprofile_config_FilterConfigSource {
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private static Class<?> clazz;

    static class FilterConfigSourceIsLoaded implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName("org.jboss.resteasy.microprofile.config.FilterConfigSource");
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }
}
