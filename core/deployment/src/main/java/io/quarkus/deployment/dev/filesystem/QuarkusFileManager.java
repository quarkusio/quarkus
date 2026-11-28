package io.quarkus.deployment.dev.filesystem;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

public abstract class QuarkusFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

    private File generatedSourcesDirectory;

    protected QuarkusFileManager(StandardJavaFileManager fileManager, Context context) {
        super(fileManager);
        this.generatedSourcesDirectory = context.getGeneratedSourcesDirectory();
        try {
            this.fileManager.setLocation(StandardLocation.CLASS_PATH, context.getClassPath());
            this.fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(context.getOutputDirectory()));
            if (context.getGeneratedSourcesDirectory() != null) {
                // Paths might be missing! (see: https://github.com/quarkusio/quarkus/issues/51178)
                ensureDirectory(context.getGeneratedSourcesDirectory());
                this.fileManager.setLocation(StandardLocation.SOURCE_OUTPUT, List.of(context.getGeneratedSourcesDirectory()));
            }
            if (context.getAnnotationProcessorPaths() != null) {
                // Paths might be missing! (see: https://github.com/quarkusio/quarkus/issues/42908)
                ensureDirectories(context.getAnnotationProcessorPaths());
                this.fileManager.setLocation(StandardLocation.ANNOTATION_PROCESSOR_PATH, context.getAnnotationProcessorPaths());
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot initialize file manager", e);
        }
    }

    public abstract Iterable<? extends JavaFileObject> getJavaSources(Iterable<? extends File> files);

    @Override
    public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
        return filterGeneratedSource(location, super.getFileForInput(location, packageName, relativeName));
    }

    protected FileObject filterGeneratedSource(Location location, FileObject file) {
        // Annotation processors use SOURCE_PATH to detect sources supplied by the application. Generated sources may
        // also be registered as compilation roots, but exposing SOURCE_OUTPUT here makes a processor mistake its own
        // output from a previous compilation for a user-provided source and skip regenerating it during hot reload.
        if (file != null && location == StandardLocation.SOURCE_PATH && isGeneratedSource(file)) {
            return null;
        }
        return file;
    }

    private boolean isGeneratedSource(FileObject file) {
        if (generatedSourcesDirectory == null || !"file".equals(file.toUri().getScheme())) {
            return false;
        }
        Path generatedSourcesPath = generatedSourcesDirectory.toPath().toAbsolutePath().normalize();
        return Path.of(file.toUri()).toAbsolutePath().normalize().startsWith(generatedSourcesPath);
    }

    public void reset(Context context) {
        this.generatedSourcesDirectory = context.getGeneratedSourcesDirectory();
        try {
            this.fileManager.setLocation(StandardLocation.CLASS_PATH, context.getClassPath());
            this.fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(context.getOutputDirectory()));
            if (context.getGeneratedSourcesDirectory() != null) {
                // Paths might be missing! (see: https://github.com/quarkusio/quarkus/issues/51178)
                ensureDirectory(context.getGeneratedSourcesDirectory());
                this.fileManager.setLocation(StandardLocation.SOURCE_OUTPUT, List.of(context.getGeneratedSourcesDirectory()));
            }
            if (context.getAnnotationProcessorPaths() != null) {
                // Paths might be missing! (see: https://github.com/quarkusio/quarkus/issues/42908)
                ensureDirectories(context.getAnnotationProcessorPaths());
                this.fileManager.setLocation(StandardLocation.ANNOTATION_PROCESSOR_PATH, context.getAnnotationProcessorPaths());
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot reset file manager", e);
        }
    }

    private void ensureDirectories(Iterable<File> directories) {
        for (File directory : directories) {
            ensureDirectory(directory);
        }
    }

    private void ensureDirectory(File directory) {
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new RuntimeException("Cannot create directory " + directory);
            }
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
