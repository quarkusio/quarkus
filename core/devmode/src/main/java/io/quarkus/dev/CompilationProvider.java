package io.quarkus.dev;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

public interface CompilationProvider {

    Set<String> handledExtensions();

    default Set<String> handledSourcePaths() {
        return Collections.emptySet();
    }

    void compile(Set<File> files, Context context);

    Path getSourcePath(Path classFilePath, Set<String> sourcePaths, String classesPath);

    class Context {

        private final String name;
        private final Set<File> classpath;
        private final File projectDirectory;
        private final File sourceDirectory;
        private final File outputDirectory;
        private final Charset sourceEncoding;

        public Context(
                String name,
                Set<File> classpath,
                File projectDirectory,
                File sourceDirectory,
                File outputDirectory,
                String sourceEncoding) {

            this.name = name;
            this.classpath = classpath;
            this.projectDirectory = projectDirectory;
            this.sourceDirectory = sourceDirectory;
            this.outputDirectory = outputDirectory;
            this.sourceEncoding = sourceEncoding == null ? StandardCharsets.UTF_8 : Charset.forName(sourceEncoding);
        }

        public String getName() {
            return name;
        }

        public Set<File> getClasspath() {
            return classpath;
        }

        public File getProjectDirectory() {
            return projectDirectory;
        }

        public File getSourceDirectory() {
            return sourceDirectory;
        }

        public File getOutputDirectory() {
            return outputDirectory;
        }

        public Charset getSourceEncoding() {
            return sourceEncoding;
        }
    }
}
