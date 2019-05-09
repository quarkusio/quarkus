package io.quarkus.dev;

import java.io.File;
import java.util.Set;

public interface CompilationProvider {

    Set<String> handledExtensions();

    void compile(Set<File> files, Context context);

    class Context {
        private final Set<File> classpath;
        private final File outputDirectory;

        public Context(Set<File> classpath, File outputDirectory) {
            this.classpath = classpath;
            this.outputDirectory = outputDirectory;
        }

        public Set<File> getClasspath() {
            return classpath;
        }

        public File getOutputDirectory() {
            return outputDirectory;
        }
    }
}
