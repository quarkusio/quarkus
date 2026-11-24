package io.quarkus.arc.test.classloading;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
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

class TestDiscoveryTransformerRetentionTest {

    @TempDir
    Path applicationDir;

    @ParameterizedTest
    @CsvSource({
            "TEST, false, false",
            "TEST, true, true",
            "DEV, false, true",
            "CONTINUOUS_TEST, false, true"
    })
    void retainTransformerForReloadButNotOrdinaryTests(QuarkusBootstrap.Mode mode, boolean auxiliaryApplication,
            boolean expectRetainedTransformer) throws Exception {
        Path applicationRoot = ShrinkWrap.create(JavaArchive.class, "transformer-retention")
                .addClass(FinalBean.class)
                .as(ExplodedExporter.class)
                .exportExplodedInto(applicationDir.toFile()).toPath();

        Field lastTransformers;
        try (CuratedApplication application = QuarkusBootstrap.builder()
                .setApplicationRoot(applicationRoot)
                .setProjectRoot(Path.of("").toAbsolutePath())
                .setTargetDirectory(Files.createDirectories(applicationDir.resolve("target")))
                .setMode(mode)
                .setTest(true)
                .setAuxiliaryApplication(auxiliaryApplication)
                .setIsolateDeployment(true)
                .addExcludedPath(PathTestHelper.getTestClassesLocation(TestDiscoveryTransformerRetentionTest.class))
                .build().bootstrap()) {
            StartupAction startup = application.createAugmentor().createInitialRuntimeApplication();
            try (QuarkusClassLoader runtimeClassLoader = startup.getClassLoader()) {
                // Augmentation must still transform the bean to allow a client proxy for its normal scope.
                Class<?> beanClass = runtimeClassLoader.loadClass(FinalBean.class.getName());
                assertThat(beanClass.getClassLoader()).isSameAs(runtimeClassLoader);
                assertThat(Modifier.isFinal(beanClass.getModifiers())).isFalse();

                // Check the actual augmentation loader before either the application or its loaders close.
                Class<?> transformingStep = application.getAugmentClassLoader()
                        .loadClass("io.quarkus.deployment.steps.ClassTransformingBuildStep");
                assertThat(transformingStep.getClassLoader()).isSameAs(application.getAugmentClassLoader());
                lastTransformers = transformingStep.getDeclaredField("lastTransformers");
                lastTransformers.setAccessible(true);
                if (expectRetainedTransformer) {
                    assertThat(lastTransformers.get(null)).isNotNull();
                } else {
                    assertThat(lastTransformers.get(null)).isNull();
                }

                try (InputStream classFile = getClass().getClassLoader()
                        .getResourceAsStream(FinalBean.class.getName().replace('.', '/') + ".class")) {
                    assertThat(classFile).isNotNull();
                    byte[] originalBytes = classFile.readAllBytes();
                    byte[] transformedBytes = (byte[]) transformingStep.getMethod("transform", String.class, byte[].class)
                            .invoke(null, FinalBean.class.getName(), originalBytes);
                    if (expectRetainedTransformer) {
                        assertThat(transformedBytes).isNotEqualTo(originalBytes);
                    } else {
                        assertThat(transformedBytes).isSameAs(originalBytes);
                    }
                }
            }
        }
        assertThat(lastTransformers.get(null)).isNull();
    }

    @Unremovable
    @ApplicationScoped
    public static final class FinalBean {
    }
}
