package io.quarkus.caffeine.runtime.graal;

import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import com.oracle.svm.core.annotate.AutomaticFeature;

/**
 * This Automatic Feature for GraalVM will register for reflection
 * the most commonly used cache implementations from Caffeine.
 * It's implemented as an explicit @{@link Feature} rather than
 * using the Quarkus builditems because it doesn't need to be
 * dynamically tuned (the list is static), and to take advantage
 * of the reachability information we can infer from @{@link org.graalvm.nativeimage.hosted.Feature.DuringAnalysisAccess}.
 *
 * This allows us to register for reflection these resources only if
 * Caffeine is indeed being used: only if the cache builder is reachable
 * in the application code.
 */
@AutomaticFeature
public class CacheConstructorsAutofeature implements Feature {

    private final AtomicBoolean triggered = new AtomicBoolean(false);

    /**
     * To set this, add `-J-Dio.quarkus.caffeine.graalvm.diagnostics=true` to the native-image parameters
     */
    private static final boolean log = Boolean.getBoolean("io.quarkus.caffeine.graalvm.diagnostics");

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        Class<?> caffeineCoreClazz = access.findClassByName("com.github.benmanes.caffeine.cache.Caffeine");
        access.registerReachabilityHandler(this::ensureCaffeineSupportEnabled, caffeineCoreClazz);
    }

    private void ensureCaffeineSupportEnabled(DuringAnalysisAccess duringAnalysisAccess) {
        final boolean needsEnablingYet = triggered.compareAndSet(false, true);
        if (needsEnablingYet) {
            if (log) {
                System.out.println(
                        "Quarkus's automatic feature for GraalVM native images: enabling support for core Caffeine caches");
            }
            registerCaffeineReflections(duringAnalysisAccess);
        }
    }

    private void registerCaffeineReflections(DuringAnalysisAccess duringAnalysisAccess) {
        final String[] needsHavingSimpleConstructors = typesNeedingConstructorsRegistered();
        for (String className : needsHavingSimpleConstructors) {
            registerForReflection(className, duringAnalysisAccess);
        }
    }

    private void registerForReflection(
            String className,
            DuringAnalysisAccess duringAnalysisAccess) {
        final Class<?> aClass = duringAnalysisAccess.findClassByName(className);
        final Constructor<?>[] z = aClass.getDeclaredConstructors();
        RuntimeReflection.register(aClass);
        RuntimeReflection.register(z);
    }

    public static String[] typesNeedingConstructorsRegistered() {
        return new String[] {
                //N.B. this list is not complete, but a selection of the types we expect being most useful.
                //unfortunately registering all of them has been shown to have a very significant impact
                //on executable sizes. See https://github.com/quarkusio/quarkus/issues/12961
                "com.github.benmanes.caffeine.cache.PDMS",
                "com.github.benmanes.caffeine.cache.PSA",
                "com.github.benmanes.caffeine.cache.PSMS",
                "com.github.benmanes.caffeine.cache.PSW",
                "com.github.benmanes.caffeine.cache.PSWMS",
                "com.github.benmanes.caffeine.cache.PSWMW",
                "com.github.benmanes.caffeine.cache.SILMS",
                "com.github.benmanes.caffeine.cache.SSA",
                "com.github.benmanes.caffeine.cache.SSLA",
                "com.github.benmanes.caffeine.cache.SSLMS",
                "com.github.benmanes.caffeine.cache.SSMS",
                "com.github.benmanes.caffeine.cache.SSMSA",
                "com.github.benmanes.caffeine.cache.SSMSW",
                "com.github.benmanes.caffeine.cache.SSW",
        };
    }

}
