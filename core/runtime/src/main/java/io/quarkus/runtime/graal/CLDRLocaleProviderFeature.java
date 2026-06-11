package io.quarkus.runtime.graal;

import java.nio.charset.StandardCharsets;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.graalvm.nativeimage.hosted.RuntimeResourceAccess;

/**
 * Registers the JDK CLDR locale data provider when Quarkus disables automatic service-loader
 * registration.
 */
public final class CLDRLocaleProviderFeature implements Feature {
    private static final String LOCALE_DATA_META_INFO = "sun.util.locale.provider.LocaleDataMetaInfo";
    private static final String CLDR_PROVIDER = "sun.util.resources.cldr.provider.CLDRLocaleDataMetaInfo";

    @Override
    public String getDescription() {
        return "Register the JDK CLDR locale data provider when automatic service-loader registration is disabled";
    }

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        // GraalVM registers this through ServiceLoaderFeature when resource bundles are runtime
        // initialized. Quarkus disables that feature by default, so register only this JDK service.
        Class<?> provider = access.findClassByName(CLDR_PROVIDER);
        if (provider == null) {
            return;
        }
        RuntimeReflection.register(provider);
        RuntimeReflection.registerForReflectiveInstantiation(provider);
        RuntimeResourceAccess.addResource(
                provider.getModule(),
                "META-INF/services/" + LOCALE_DATA_META_INFO,
                (CLDR_PROVIDER + "\n").getBytes(StandardCharsets.UTF_8));
    }
}
