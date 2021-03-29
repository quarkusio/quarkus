package io.quarkus.deployment.dev;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.jboss.logging.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.gizmo.Gizmo;

public class JavaCompilationProvider implements CompilationProvider {

    private static final Logger log = Logger.getLogger(JavaCompilationProvider.class);

    // -g is used to make the java compiler generate all debugging info
    // -parameters is used to generate metadata for reflection on method parameters
    // this is useful when people using debuggers against their hot-reloaded app
    private static final Set<String> COMPILER_OPTIONS = new HashSet<>(Arrays.asList("-g", "-parameters"));

    JavaCompiler compiler;
    StandardJavaFileManager fileManager;
    DiagnosticCollector<JavaFileObject> fileManagerDiagnostics;

    @Override
    public Set<String> handledExtensions() {
        return Collections.singleton(".java");
    }

    @Override
    public void compile(Set<File> filesToCompile, Context context) {
        JavaCompiler compiler = this.compiler;
        if (compiler == null) {
            compiler = this.compiler = ToolProvider.getSystemJavaCompiler();
        }
        if (compiler == null) {
            throw new RuntimeException("No system java compiler provided");
        }
        try {
            if (fileManager == null) {
                fileManager = compiler.getStandardFileManager(fileManagerDiagnostics = new DiagnosticCollector<>(), null,
                        context.getSourceEncoding());
            }

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            fileManager.setLocation(StandardLocation.CLASS_PATH, context.getClasspath());
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singleton(context.getOutputDirectory()));

            CompilerFlags compilerFlags = new CompilerFlags(COMPILER_OPTIONS, context.getCompilerOptions(),
                    context.getSourceJavaVersion(), context.getTargetJvmVersion());

            Iterable<? extends JavaFileObject> sources = fileManager.getJavaFileObjectsFromFiles(filesToCompile);
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics,
                    compilerFlags.toList(), null, sources);

            if (!task.call()) {
                throw new RuntimeException("Compilation failed" + diagnostics.getDiagnostics());
            }

            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                log.logf(diagnostic.getKind() == Diagnostic.Kind.ERROR ? Logger.Level.ERROR : Logger.Level.WARN,
                        "%s, line %d in %s", diagnostic.getMessage(null), diagnostic.getLineNumber(),
                        diagnostic.getSource() == null ? "[unknown source]" : diagnostic.getSource().getName());
            }
            if (!fileManagerDiagnostics.getDiagnostics().isEmpty()) {
                for (Diagnostic<? extends JavaFileObject> diagnostic : fileManagerDiagnostics.getDiagnostics()) {
                    log.logf(diagnostic.getKind() == Diagnostic.Kind.ERROR ? Logger.Level.ERROR : Logger.Level.WARN,
                            "%s, line %d in %s", diagnostic.getMessage(null), diagnostic.getLineNumber(),
                            diagnostic.getSource() == null ? "[unknown source]" : diagnostic.getSource().getName());
                }
                fileManager.close();
                fileManagerDiagnostics = null;
                fileManager = null;
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot close file manager", e);
        }
    }

    @Override
    public Path getSourcePath(Path classFilePath, Set<String> sourcePaths, String classesPath) {
        Path sourceFilePath = null;
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
        if (fileManager != null) {
            fileManager.close();
            fileManager = null;
            fileManagerDiagnostics = null;
        }
    }

    static class RuntimeUpdatesClassVisitor extends ClassVisitor {
        private Set<String> sourcePaths;
        private String classesPath;
        private String sourceFile;

        public RuntimeUpdatesClassVisitor(Set<String> sourcePaths, String classesPath) {
            super(Gizmo.ASM_API_VERSION);
            this.sourcePaths = sourcePaths;
            this.classesPath = classesPath;
        }

        @Override
        public void visitSource(String source, String debug) {
            this.sourceFile = source;
        }

        public Path getSourceFileForClass(final Path classFilePath) {
            for (String moduleSourcePath : sourcePaths) {
                final Path sourcesDir = Paths.get(moduleSourcePath);
                final Path classesDir = Paths.get(classesPath);
                final StringBuilder sourceRelativeDir = new StringBuilder();
                sourceRelativeDir.append(classesDir.relativize(classFilePath.getParent()));
                sourceRelativeDir.append(File.separator);
                sourceRelativeDir.append(sourceFile);
                final Path sourceFilePath = sourcesDir.resolve(Paths.get(sourceRelativeDir.toString()));
                if (Files.exists(sourceFilePath)) {
                    return sourceFilePath;
                }
            }

            return null;
        }
    }

}
