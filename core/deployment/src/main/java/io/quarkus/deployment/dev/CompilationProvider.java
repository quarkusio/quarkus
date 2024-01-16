package io.quarkus.deployment.dev;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.paths.PathCollection;

public interface CompilationProvider extends Closeable {

    Set<String> handledExtensions();

    default String getProviderKey() {
        return getClass().getName();
    }

    default Set<String> handledSourcePaths() {
        return Collections.emptySet();
    }

    void compile(Set<File> files, Context context);

    Path getSourcePath(Path classFilePath, PathCollection sourcePaths, String classesPath);

    @Override
    default void close() throws IOException {

    }

    class Context {

        private final String name;
        private final Set<File> classpath;
        private final Set<File> reloadableClasspath;
        private final File projectDirectory;
        private final File sourceDirectory;
        private final File outputDirectory;
        private final Charset sourceEncoding;
        private final Map<String, Set<String>> compilerOptions;
        private final String releaseJavaVersion;
        private final String sourceJavaVersion;
        private final String targetJvmVersion;
        private final List<String> compilePluginArtifacts;
        private final List<String> compilerPluginOptions;
        private final boolean ignoreModuleInfo;
        private final File generatedSourcesDirectory;
        private final Set<File> annotationProcessorPaths;
        private final List<String> annotationProcessors;

        public Context(
                String name,
                Set<File> classpath,
                Set<File> reloadableClasspath,
                File projectDirectory,
                File sourceDirectory,
                File outputDirectory,
                String sourceEncoding,
                Map<String, Set<String>> compilerOptions,
                String releaseJavaVersion,
                String sourceJavaVersion,
                String targetJvmVersion,
                List<String> compilePluginArtifacts,
                List<String> compilerPluginOptions,
                File generatedSourcesDirectory,
                Set<File> annotationProcessorPaths,
                List<String> annotationProcessors,
                String ignoreModuleInfo) {
            this.name = name;
            this.classpath = classpath;
            this.reloadableClasspath = reloadableClasspath;
            this.projectDirectory = projectDirectory;
            this.sourceDirectory = sourceDirectory;
            this.outputDirectory = outputDirectory;
            this.sourceEncoding = sourceEncoding == null ? StandardCharsets.UTF_8 : Charset.forName(sourceEncoding);
            this.compilerOptions = compilerOptions == null ? new HashMap<>() : compilerOptions;
            this.releaseJavaVersion = releaseJavaVersion;
            this.sourceJavaVersion = sourceJavaVersion;
            this.targetJvmVersion = targetJvmVersion;
            this.compilePluginArtifacts = compilePluginArtifacts;
            this.compilerPluginOptions = compilerPluginOptions;
            this.ignoreModuleInfo = Boolean.parseBoolean(ignoreModuleInfo);
            this.generatedSourcesDirectory = generatedSourcesDirectory;
            this.annotationProcessorPaths = annotationProcessorPaths;
            this.annotationProcessors = annotationProcessors;
        }

        public String getName() {
            return name;
        }

        public Set<File> getClasspath() {
            return classpath;
        }

        public Set<File> getReloadableClasspath() {
            return reloadableClasspath;
        }

        public Set<File> getAnnotationProcessorPaths() {
            return annotationProcessorPaths;
        }

        public List<String> getAnnotationProcessors() {
            return annotationProcessors;
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

        public Set<String> getCompilerOptions(String key) {
            return compilerOptions.getOrDefault(key, Collections.emptySet());
        }

        public String getReleaseJavaVersion() {
            return releaseJavaVersion;
        }

        public String getSourceJavaVersion() {
            return sourceJavaVersion;
        }

        public String getTargetJvmVersion() {
            return targetJvmVersion;
        }

        public List<String> getCompilePluginArtifacts() {
            return compilePluginArtifacts;
        }

        public List<String> getCompilerPluginOptions() {
            return compilerPluginOptions;
        }

        public boolean ignoreModuleInfo() {
            return ignoreModuleInfo;
        }

        public File getGeneratedSourcesDirectory() {
            return generatedSourcesDirectory;
        }
    }
}
