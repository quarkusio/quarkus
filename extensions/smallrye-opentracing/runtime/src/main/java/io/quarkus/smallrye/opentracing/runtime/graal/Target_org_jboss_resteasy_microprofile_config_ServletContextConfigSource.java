package io.quarkus.smallrye.opentracing.runtime.graal;

import org.jboss.resteasy.microprofile.config.ServletContextConfigSource;
import org.jboss.resteasy.microprofile.config.ServletContextConfigSourceImpl;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * We need this class to ensure that {@link ServletContextConfigSourceImpl}
 * isn't loaded unless Undertow has been explicitly added.
 *
 * This is done because the OpenTracing extension depends on javax.servlet which results in
 * {@link ServletContextConfigSourceImpl}
 * being loaded (due to what {@link ServletContextConfigSource} checks for), which in turns leads to
 * GraalVM failing because the class cannot be instantiated.
 *
 * See https://github.com/quarkusio/quarkus/issues/9086
 */
@TargetClass(value = ServletContextConfigSource.class, onlyWith = UndertowMissing.class)
final class Target_org_jboss_resteasy_microprofile_config_ServletContextConfigSource {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static boolean SERVLET_AVAILABLE = false;

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private static Class<?> clazz;
}
