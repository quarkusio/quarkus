package io.quarkus.hibernate.orm.runtime.graal;

import java.util.HashSet;
import java.util.Set;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

/**
 * Makes methods of reachable hibernate services accessible through {@link Class#getMethods()}.
 *
 * See <a href="https://github.com/quarkusio/quarkus/issues/45525">Github issue #45525</a>.
 */
public class RegisterServicesForReflectionFeature implements Feature {

    @Override
    public String getDescription() {
        return "Makes methods of reachable hibernate services accessible through getMethods()`";
    }

    // The {@code duringAnalysis} method is invoked multiple times and increases the set of reachable types, thus we
    // need to invoke {@link DuringAnalysisAccess#requireAnalysisIteration()} each time we register new methods.
    private static final int ANTICIPATED_SERVICES = 100;
    private static final Set<Class<?>> registeredClasses = new HashSet<>(ANTICIPATED_SERVICES);

    @Override
    public void duringAnalysis(DuringAnalysisAccess access) {
        for (Class<?> service : access.reachableSubtypes(org.hibernate.service.Service.class)) {
            if (registeredClasses.add(service)) {
                RuntimeReflection.registerAllMethods(service);
                access.requireAnalysisIteration();
            }
        }
    }
}
