package io.quarkus.test.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.RunningQuarkusApplication;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.common.WithTestResource;

class AbstractJvmQuarkusTestExtensionTest {

    @Test
    void reusesApplicationForDifferentClassLoaderFromSameCuratedApplication() {
        QuarkusClassLoader applicationClassLoader = mock(QuarkusClassLoader.class);
        QuarkusClassLoader testClassLoader = mock(QuarkusClassLoader.class);
        CuratedApplication curatedApplication = mock(CuratedApplication.class);

        when(applicationClassLoader.getCuratedApplication()).thenReturn(curatedApplication);
        when(testClassLoader.getCuratedApplication()).thenReturn(curatedApplication);

        assertThat(AbstractJvmQuarkusTestExtension.isSameApplication(applicationClassLoader, testClassLoader)).isTrue();
    }

    @Test
    void restartsWhenTestResourceChanges() {
        RunningQuarkusApplication previous = AbstractJvmQuarkusTestExtension.runningQuarkusApplication;
        RunningQuarkusApplication running = mock(RunningQuarkusApplication.class);
        QuarkusTestExtensionState state = mock(QuarkusTestExtensionState.class);
        when(running.getClassLoader()).thenReturn(ResourceTest.class.getClassLoader());
        AbstractJvmQuarkusTestExtension.runningQuarkusApplication = running;

        try {
            assertThat(new AbstractJvmQuarkusTestExtension().isNewApplication(state, ResourceTest.class, true)).isTrue();
        } finally {
            AbstractJvmQuarkusTestExtension.runningQuarkusApplication = previous;
        }
    }

    @WithTestResource(DummyResource.class)
    private static class ResourceTest {
    }

    private abstract static class DummyResource implements QuarkusTestResourceLifecycleManager {
    }
}
