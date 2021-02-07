package io.quarkus.smallrye.opentracing.runtime.graal;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * see {@link Target_org_jboss_resteasy_microprofile_config_ServletContextConfigSource}
 */
@TargetClass(className = Target_org_jboss_resteasy_microprofile_config_ServletConfigSource.SERVLET_CONFIG_SOURCE_NAME, onlyWith = {
        UndertowMissing.class,
        Target_org_jboss_resteasy_microprofile_config_ServletConfigSource.ServletConfigSourceIsLoaded.class,
})
final class Target_org_jboss_resteasy_microprofile_config_ServletConfigSource {

    static final String SERVLET_CONFIG_SOURCE_NAME = "org.jboss.resteasy.microprofile.config.ServletConfigSource";

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static boolean SERVLET_AVAILABLE = false;

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private static Class<?> clazz;

    static class ServletConfigSourceIsLoaded implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName(SERVLET_CONFIG_SOURCE_NAME);
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }
}
