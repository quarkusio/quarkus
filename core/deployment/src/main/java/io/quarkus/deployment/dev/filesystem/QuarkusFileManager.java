package io.quarkus.deployment.dev.filesystem;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;

import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

public abstract class QuarkusFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

    protected QuarkusFileManager(StandardJavaFileManager fileManager, Context context) {
        super(fileManager);
        try {
            this.fileManager.setLocation(StandardLocation.CLASS_PATH, context.getClassPath());
            this.fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(context.getOutputDirectory()));
            if (context.getGeneratedSourcesDirectory() != null) {
                this.fileManager.setLocation(StandardLocation.SOURCE_OUTPUT, List.of(context.getGeneratedSourcesDirectory()));
            }
            if (context.getAnnotationProcessorPaths() != null) {
                this.fileManager.setLocation(StandardLocation.ANNOTATION_PROCESSOR_PATH, context.getAnnotationProcessorPaths());
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot initialize file manager", e);
        }
    }

    public abstract Iterable<? extends JavaFileObject> getJavaSources(Iterable<? extends File> files);

    public void reset(Context context) {
        try {
            this.fileManager.setLocation(StandardLocation.CLASS_PATH, context.getClassPath());
            this.fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(context.getOutputDirectory()));
            if (context.getGeneratedSourcesDirectory() != null) {
                this.fileManager.setLocation(StandardLocation.SOURCE_OUTPUT, List.of(context.getGeneratedSourcesDirectory()));
            }
            if (context.getAnnotationProcessorPaths() != null) {
                this.fileManager.setLocation(StandardLocation.ANNOTATION_PROCESSOR_PATH, context.getAnnotationProcessorPaths());
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot reset file manager", e);
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    public static class Context {
        private final Set<File> classPath;
        private final Set<File> reloadableClassPath;
        private final File outputDirectory;
        private final Charset sourceEncoding;
        private final boolean ignoreModuleInfo;
        private final File generatedSourcesDirectory;
        private final Set<File> annotationProcessorPaths;

        public Context(Set<File> classPath, Set<File> reloadableClassPath,
                File outputDirectory, File generatedSourcesDirectory, Set<File> annotationProcessorPaths,
                Charset sourceEncoding, boolean ignoreModuleInfo) {
            this.classPath = classPath;
            this.reloadableClassPath = reloadableClassPath;
            this.outputDirectory = outputDirectory;
            this.sourceEncoding = sourceEncoding;
            this.ignoreModuleInfo = ignoreModuleInfo;
            this.generatedSourcesDirectory = generatedSourcesDirectory;
            this.annotationProcessorPaths = annotationProcessorPaths;
        }

        public Set<File> getAnnotationProcessorPaths() {
            return annotationProcessorPaths;
        }

        public Set<File> getClassPath() {
            return classPath;
        }

        public Set<File> getReloadableClassPath() {
            return reloadableClassPath;
        }

        public File getOutputDirectory() {
            return outputDirectory;
        }

        public Charset getSourceEncoding() {
            return sourceEncoding;
        }

        public boolean ignoreModuleInfo() {
            return ignoreModuleInfo;
        }

        public File getGeneratedSourcesDirectory() {
            return generatedSourcesDirectory;
        }
    }
}
