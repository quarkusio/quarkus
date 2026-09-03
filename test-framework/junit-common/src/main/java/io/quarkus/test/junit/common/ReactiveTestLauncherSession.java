package io.quarkus.test.junit.common;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * A {@link LauncherSessionListener} that wraps the TCCL with a
 * {@link ReactiveTestTransformingClassLoader} to transform {@code Uni}-returning
 * {@code @Test @TestTransaction} methods into synthetic void stubs for JUnit discovery.
 * <p>
 * When the {@code FacadeClassLoader} (from {@code quarkus-junit}) is also on the classpath,
 * this wrapping still applies: {@code FacadeClassLoader} is created on top of the
 * transforming classloader and delegates to it for non-QuarkusTest classes.
 */
public class ReactiveTestLauncherSession implements LauncherSessionListener {

    private ClassLoader originalTccl;

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        originalTccl = Thread.currentThread().getContextClassLoader();
        ClassLoader wrapper = new ReactiveTestTransformingClassLoader(originalTccl);
        Thread.currentThread().setContextClassLoader(wrapper);
        // If ConfigLauncherSession already ran, config is registered for originalTccl
        // but not for our wrapper. Register it so lookups via the new TCCL succeed.
        registerConfigForClassLoader(wrapper);
    }

    @Override
    public void launcherSessionClosed(LauncherSession session) {
        if (originalTccl != null) {
            Thread.currentThread().setContextClassLoader(originalTccl);
            originalTccl = null;
        }
    }

    private static void registerConfigForClassLoader(ClassLoader classLoader) {
        try {
            ConfigProviderResolver resolver = ConfigProviderResolver.instance();
            // Get config registered for the parent (original TCCL) and register it
            // for the wrapper too, so lookups by the new TCCL succeed.
            resolver.registerConfig(resolver.getConfig(classLoader.getParent()), classLoader);
        } catch (IllegalStateException ignored) {
            // No ConfigProviderResolver set yet (we ran before ConfigLauncherSession) — that's
            // fine, ConfigLauncherSession will see our wrapper as the TCCL and register for it.
        }
    }
}
