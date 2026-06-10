package io.quarkus.hibernate.orm.runtime.graal;

import java.util.HashSet;
import java.util.Set;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.hibernate.persister.state.spi.StateManagement;

/**
 * Makes fields of reachable Hibernate StateManagement implementations accessible through
 * {@link Class#getDeclaredField(String)}.
 * <p>
 * Hibernate ORM 7.4+ uses reflection to access the static {@code INSTANCE} field of
 * StateManagement implementations at runtime. This feature ensures those fields are
 * available for reflection in native mode.
 * <p>
 * See <a href="https://github.com/quarkusio/quarkus/issues/54777">GitHub issue #54777</a>.
 */
public class RegisterStateManagementForReflectionFeature implements Feature {

    @Override
    public String getDescription() {
        return "Makes fields of reachable Hibernate StateManagement implementations accessible through Class#getDeclaredField()";
    }

    // The {@code duringAnalysis} method is invoked multiple times and increases the set of reachable types, thus we
    // need to invoke {@link DuringAnalysisAccess#requireAnalysisIteration()} each time we register new fields.
    private static final int ANTICIPATED_STATE_MANAGEMENT_CLASSES = 10;
    private static final Set<Class<?>> registeredClasses = new HashSet<>(ANTICIPATED_STATE_MANAGEMENT_CLASSES);

    @Override
    public void duringAnalysis(DuringAnalysisAccess access) {
        for (Class<?> stateManagement : access.reachableSubtypes(StateManagement.class)) {
            if (registeredClasses.add(stateManagement)) {
                RuntimeReflection.registerAllDeclaredFields(stateManagement);
                access.requireAnalysisIteration();
            }
        }
    }
}
