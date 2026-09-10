package io.quarkus.test.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;

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
}
