package io.quarkus.deployment.dev;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.jboss.logging.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.deployment.dev.filesystem.QuarkusFileManager;
import io.quarkus.deployment.dev.filesystem.ReloadableFileManager;
import io.quarkus.deployment.dev.filesystem.StaticFileManager;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.paths.PathCollection;

public class JavaCompilationProvider implements CompilationProvider {

    private static final Logger LOG = Logger.getLogger(JavaCompilationProvider.class);

    // -g is used to make the java compiler generate all debugging info
    // -parameters is used to generate metadata for reflection on method parameters
    // this is useful when people using debuggers against their hot-reloaded app
    private static final Set<String> COMPILER_OPTIONS = Set.of("-g", "-parameters");
    private static final Set<String> IGNORE_NAMESPACES = Set.of("org.osgi", "Annotation processing is enabled because");

    private static final String PROVIDER_KEY = "java";

    private JavaCompiler compiler;
    private List<String> compilerFlags;
    private QuarkusFileManager fileManager;

    @Override
    public String getProviderKey() {
        return PROVIDER_KEY;
    }

    @Override
    public Set<String> handledExtensions() {
        return Set.of(".java");
    }

    @Override
    public void compile(Set<File> filesToCompile, CompilationProvider.Context context) {
        if (this.compiler == null) {
            this.compiler = ToolProvider.getSystemJavaCompiler();
            this.compilerFlags = new CompilerFlags(COMPILER_OPTIONS,
                    context.getCompilerOptions(PROVIDER_KEY),
                    context.getReleaseJavaVersion(),
                    context.getSourceJavaVersion(),
                    context.getTargetJvmVersion(),
                    context.getAnnotationProcessors()).toList();
        }

        final JavaCompiler compiler = this.compiler;

        if (compiler == null) {
            throw new RuntimeException("No system java compiler provided");
        }

        final QuarkusFileManager.Context sourcesContext = new QuarkusFileManager.Context(
                context.getClasspath(), context.getReloadableClasspath(),
                context.getOutputDirectory(), context.getGeneratedSourcesDirectory(),
                context.getAnnotationProcessorPaths(),
                context.getSourceEncoding(),
                context.ignoreModuleInfo());

        if (this.fileManager == null) {
            final Supplier<StandardJavaFileManager> supplier = () -> {
                final Charset charset = context.getSourceEncoding();
                return compiler.getStandardFileManager(null, null, charset);
            };

            if (context.getReloadableClasspath().isEmpty()) {
                this.fileManager = new StaticFileManager(supplier, sourcesContext);
            } else {
                this.fileManager = new ReloadableFileManager(supplier, sourcesContext);
            }
        } else {
            this.fileManager.reset(sourcesContext);
        }

        final DiagnosticCollector<JavaFileObject> diagnosticsCollector = new DiagnosticCollector<>();

        final Iterable<? extends JavaFileObject> sources = this.fileManager.getJavaSources(filesToCompile);
        final JavaCompiler.CompilationTask task = this.compiler.getTask(null, this.fileManager,
                diagnosticsCollector, this.compilerFlags, null, sources);

        final boolean compilationTaskSucceed = task.call();

        if (LOG.isEnabled(Logger.Level.ERROR) || LOG.isEnabled(Logger.Level.WARN)) {
            collectDiagnostics(diagnosticsCollector, (level, diagnostic) -> LOG.logf(level, "%s, line %d in %s",
                    diagnostic.getMessage(null), diagnostic.getLineNumber(),
                    diagnostic.getSource() == null ? "[unknown source]" : diagnostic.getSource().getName()));
        }

        if (!compilationTaskSucceed) {
            final String errorMessage = extractCompilationErrorMessage(diagnosticsCollector);
            throw new RuntimeException(errorMessage);
        }
    }

    @Override
    public Path getSourcePath(Path classFilePath, PathCollection sourcePaths, String classesPath) {
        Path sourceFilePath;
        final RuntimeUpdatesClassVisitor visitor = new RuntimeUpdatesClassVisitor(sourcePaths, classesPath);
        try (final InputStream inputStream = Files.newInputStream(classFilePath)) {
            final ClassReader reader = new ClassReader(inputStream);
            reader.accept(visitor, 0);
            sourceFilePath = visitor.getSourceFileForClass(classFilePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sourceFilePath;
    }

    @Override
    public void close() throws IOException {
        if (this.fileManager != null) {
            this.fileManager.close();
            this.fileManager = null;
        }
    }

    private void collectDiagnostics(final DiagnosticCollector<JavaFileObject> diagnosticsCollector,
            final BiConsumer<Logger.Level, Diagnostic<? extends JavaFileObject>> callback) {
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnosticsCollector.getDiagnostics()) {
            Logger.Level level = diagnostic.getKind() == Diagnostic.Kind.ERROR ? Logger.Level.ERROR : Logger.Level.WARN;
            if (level.equals(Logger.Level.WARN) && IGNORE_NAMESPACES.stream()
                    .anyMatch(diagnostic.getMessage(null)::contains)) {
                continue;
            }
            callback.accept(level, diagnostic);
        }
    }

    private String extractCompilationErrorMessage(final DiagnosticCollector<JavaFileObject> diagnosticsCollector) {
        StringBuilder builder = new StringBuilder();
        diagnosticsCollector.getDiagnostics().forEach(diagnostic -> builder.append("\n").append(diagnostic));
        return String.format("\u001B[91mCompilation Failed:%s\u001b[0m", builder);
    }

    private static class RuntimeUpdatesClassVisitor extends ClassVisitor {
        private final PathCollection sourcePaths;
        private final String classesPath;
        private String sourceFile;

        public RuntimeUpdatesClassVisitor(PathCollection sourcePaths, String classesPath) {
            super(Gizmo.ASM_API_VERSION);
            this.sourcePaths = sourcePaths;
            this.classesPath = classesPath;
        }

        @Override
        public void visitSource(String source, String debug) {
            this.sourceFile = source;
        }

        public Path getSourceFileForClass(final Path classFilePath) {
            for (Path sourcesDir : sourcePaths) {
                final Path classesDir = Paths.get(classesPath);
                final StringBuilder sourceRelativeDir = new StringBuilder();
                sourceRelativeDir.append(classesDir.relativize(classFilePath.getParent()));
                sourceRelativeDir.append(File.separator);
                sourceRelativeDir.append(sourceFile);
                final Path sourceFilePath = sourcesDir.resolve(Path.of(sourceRelativeDir.toString()));
                if (Files.exists(sourceFilePath)) {
                    return sourceFilePath;
                }
            }
            return null;
        }
    }
}
