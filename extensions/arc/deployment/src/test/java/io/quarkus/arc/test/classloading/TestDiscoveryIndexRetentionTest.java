package io.quarkus.arc.test.classloading;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.arc.Unremovable;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.app.StartupAction;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.test.common.PathTestHelper;

class TestDiscoveryIndexRetentionTest {

    @TempDir
    Path applicationDir;

    @ParameterizedTest
    @CsvSource({
            "TEST, false, false",
            "TEST, true, true",
            "DEV, false, true",
            "CONTINUOUS_TEST, false, true"
    })
    void retainIndexOnlyWhenNeededForReload(QuarkusBootstrap.Mode mode, boolean auxiliaryApplication,
            boolean expectRetainedIndex) throws Exception {
        Path applicationRoot = ShrinkWrap.create(JavaArchive.class, "index-retention")
                .addClass(FinalBean.class)
                .as(ExplodedExporter.class)
                .exportExplodedInto(applicationDir.toFile()).toPath();

        try (CuratedApplication application = QuarkusBootstrap.builder()
                .setApplicationRoot(applicationRoot)
                .setProjectRoot(Path.of("").toAbsolutePath())
                .setTargetDirectory(Files.createDirectories(applicationDir.resolve("target")))
                .setMode(mode)
                .setTest(true)
                .setAuxiliaryApplication(auxiliaryApplication)
                .setIsolateDeployment(true)
                .addExcludedPath(PathTestHelper.getTestClassesLocation(TestDiscoveryIndexRetentionTest.class))
                .build().bootstrap()) {
            StartupAction startup = application.createAugmentor().createInitialRuntimeApplication();
            try (QuarkusClassLoader runtimeClassLoader = startup.getClassLoader()) {
                // Check the actual augmentation loader before either the application or its loaders close.
                Class<?> updatesProcessor = application.getAugmentClassLoader()
                        .loadClass("io.quarkus.deployment.dev.RuntimeUpdatesProcessor");
                assertThat(updatesProcessor.getClassLoader()).isSameAs(application.getAugmentClassLoader());
                Field lastStartIndex = updatesProcessor.getDeclaredField("lastStartIndex");
                lastStartIndex.setAccessible(true);
                if (expectRetainedIndex) {
                    Object retainedIndex = lastStartIndex.get(null);
                    assertThat(retainedIndex).isNotNull();
                    assertThat(retainedIndex.getClass().getMethod("getClassByName", String.class)
                            .invoke(retainedIndex, FinalBean.class.getName())).isNotNull();
                } else {
                    assertThat(lastStartIndex.get(null)).isNull();
                }

                // ArC must still transform the bean to allow a client proxy for its normal scope.
                Class<?> beanClass = runtimeClassLoader.loadClass(FinalBean.class.getName());
                assertThat(beanClass.getClassLoader()).isSameAs(runtimeClassLoader);
                assertThat(Modifier.isFinal(beanClass.getModifiers())).isFalse();
            }
        }
    }

    @Unremovable
    @ApplicationScoped
    public static final class FinalBean {
    }
}
