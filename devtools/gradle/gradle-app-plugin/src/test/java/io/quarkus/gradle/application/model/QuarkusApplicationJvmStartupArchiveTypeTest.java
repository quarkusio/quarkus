package io.quarkus.gradle.application.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QuarkusApplicationJvmStartupArchiveTypeTest {

    @Test
    void modelsOpenJdkAotAsAFileArchive() {
        QuarkusApplicationJvmStartupArchiveType type = QuarkusApplicationJvmStartupArchiveType.AOT;

        assertThat(type.getQuarkusType()).isEqualTo("aot");
        assertThat(type.getCoreType()).isEqualTo("AOT");
        assertThat(type.getDefaultName()).isEqualTo("app.aot");
        assertThat(type.isDirectory()).isFalse();
        assertThat(type.getDefaultImageSuffix()).isEqualTo("-aot");
    }

    @Test
    void modelsSemeruSccAsADirectoryArchive() {
        QuarkusApplicationJvmStartupArchiveType type = QuarkusApplicationJvmStartupArchiveType.SCC;

        assertThat(type.getQuarkusType()).isEqualTo("scc");
        assertThat(type.getCoreType()).isEqualTo("SCC");
        assertThat(type.getDefaultName()).isEqualTo("app-scc");
        assertThat(type.isDirectory()).isTrue();
        assertThat(type.getDefaultImageSuffix()).isEqualTo("-scc");
    }

    @Test
    void modelsAppCdsAsAFileArchive() {
        QuarkusApplicationJvmStartupArchiveType type = QuarkusApplicationJvmStartupArchiveType.APP_CDS;

        assertThat(type.getQuarkusType()).isEqualTo("app-cds");
        assertThat(type.getCoreType()).isEqualTo("AppCDS");
        assertThat(type.getDefaultName()).isEqualTo("app-cds.jsa");
        assertThat(type.isDirectory()).isFalse();
        assertThat(type.getDefaultImageSuffix()).isEqualTo("-app-cds");
    }
}
