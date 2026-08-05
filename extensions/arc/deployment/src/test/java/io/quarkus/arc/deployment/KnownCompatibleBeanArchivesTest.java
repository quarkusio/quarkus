package io.quarkus.arc.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;

public class KnownCompatibleBeanArchivesTest {

    @Test
    void defaultTypeMatchesJarArchive() {
        KnownCompatibleBeanArchiveBuildItem item = KnownCompatibleBeanArchiveBuildItem
                .builder("org.acme", "acme-lib")
                .addReason(KnownCompatibleBeanArchiveBuildItem.Reason.BEANS_XML_ALL)
                .build();
        KnownCompatibleBeanArchives archives = new KnownCompatibleBeanArchives(List.of(item));

        assertThat(archives.isKnownCompatible(ArtifactKey.of("org.acme", "acme-lib", "", ArtifactCoords.TYPE_JAR),
                KnownCompatibleBeanArchiveBuildItem.Reason.BEANS_XML_ALL)).isTrue();
    }

    @Test
    void customTypeMatchesArchive() {
        KnownCompatibleBeanArchiveBuildItem item = KnownCompatibleBeanArchiveBuildItem
                .builder("org.acme", "acme-lib")
                .setType("test-jar")
                .addReason(KnownCompatibleBeanArchiveBuildItem.Reason.BEANS_XML_ALL)
                .build();
        KnownCompatibleBeanArchives archives = new KnownCompatibleBeanArchives(List.of(item));

        assertThat(archives.isKnownCompatible(ArtifactKey.of("org.acme", "acme-lib", "", "test-jar"),
                KnownCompatibleBeanArchiveBuildItem.Reason.BEANS_XML_ALL)).isTrue();
    }

    @Test
    void typeMismatchDoesNotMatch() {
        KnownCompatibleBeanArchiveBuildItem item = KnownCompatibleBeanArchiveBuildItem
                .builder("org.acme", "acme-lib")
                .addReason(KnownCompatibleBeanArchiveBuildItem.Reason.BEANS_XML_ALL)
                .build();
        KnownCompatibleBeanArchives archives = new KnownCompatibleBeanArchives(List.of(item));

        assertThat(archives.isKnownCompatible(ArtifactKey.of("org.acme", "acme-lib", "", "test-jar"),
                KnownCompatibleBeanArchiveBuildItem.Reason.BEANS_XML_ALL)).isFalse();
    }

    @Test
    void classifierAndTypeMatch() {
        KnownCompatibleBeanArchiveBuildItem item = KnownCompatibleBeanArchiveBuildItem
                .builder("org.acme", "acme-lib")
                .setClassifier("tests")
                .setType("test-jar")
                .addReason(KnownCompatibleBeanArchiveBuildItem.Reason.SPECIALIZES_ANNOTATION)
                .build();
        KnownCompatibleBeanArchives archives = new KnownCompatibleBeanArchives(List.of(item));

        assertThat(archives.isKnownCompatible(ArtifactKey.of("org.acme", "acme-lib", "tests", "test-jar"),
                KnownCompatibleBeanArchiveBuildItem.Reason.SPECIALIZES_ANNOTATION)).isTrue();
        assertThat(archives.isKnownCompatible(ArtifactKey.of("org.acme", "acme-lib", "", "test-jar"),
                KnownCompatibleBeanArchiveBuildItem.Reason.SPECIALIZES_ANNOTATION)).isFalse();
        assertThat(archives.isKnownCompatible(ArtifactKey.of("org.acme", "acme-lib", "tests", ArtifactCoords.TYPE_JAR),
                KnownCompatibleBeanArchiveBuildItem.Reason.SPECIALIZES_ANNOTATION)).isFalse();
    }
}
