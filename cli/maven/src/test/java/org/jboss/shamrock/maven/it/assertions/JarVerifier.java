package org.jboss.shamrock.maven.it.assertions;

import java.io.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import static org.assertj.core.api.Assertions.assertThat;

class JarVerifier {

    private File jarFile;

    JarVerifier(File jarFile) {
        this.jarFile = jarFile;
    }

    void assertThatJarIsCreated() {
        assertThat(jarFile).isNotNull();
        assertThat(jarFile).isFile();
    }

    void assertThatJarHasManifest() throws Exception {
        try (JarFile jf = new JarFile(jarFile)) {
            Manifest manifest = jf.getManifest();
            assertThat(manifest).isNotNull();
        }
    }

    void assertThatFileIsContained(String file) throws Exception {
        try (JarFile jf = new JarFile(jarFile)) {
            assertThat(jf.getJarEntry(file)).isNotNull();
        }
    }

    void assertThatFileIsNotContained(String file) throws Exception {
        try (JarFile jf = new JarFile(jarFile)) {
            assertThat(jf.getJarEntry(file)).isNull();
        }
    }

    void assertThatFileContains(String path, String[] lines) throws Exception {
        try (JarFile jf = new JarFile(jarFile)) {
            ZipEntry entry = jf.getEntry(path);
            assertThat(entry).isNotNull();
            String content = read(jf.getInputStream(entry));
            assertThat(content).containsSubsequence(lines);
        }
    }

    private static String read(InputStream input) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
            return buffer.lines().collect(Collectors.joining("\n"));
        }
    }
}
