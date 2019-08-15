package io.quarkus.dev;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class JavaCompilationProvider implements CompilationProvider {

    // -g is used to make the java compiler generate all debugging info
    // -parameters is used to generate metadata for reflection on method parameters
    // this is useful when people using debuggers against their hot-reloaded app
    private static final List<String> COMPILER_OPTIONS = Arrays.asList("-g", "-parameters");

    @Override
    public Set<String> handledExtensions() {
        return Collections.singleton(".java");
    }

    @Override
    public void compile(Set<File> filesToCompile, Context context) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("No system java compiler provided");
        }
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null,
                context.getSourceEncoding())) {

            fileManager.setLocation(StandardLocation.CLASS_PATH, context.getClasspath());
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singleton(context.getOutputDirectory()));

            Iterable<? extends JavaFileObject> sources = fileManager.getJavaFileObjectsFromFiles(filesToCompile);
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics,
                    COMPILER_OPTIONS, null, sources);

            if (!task.call()) {
                throw new RuntimeException("Compilation failed" + diagnostics.getDiagnostics());
            }

            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                System.out.format("%s, line %d in %s", diagnostic.getMessage(null), diagnostic.getLineNumber(),
                        diagnostic.getSource() == null ? "[unknown source]" : diagnostic.getSource().getName());
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

    class RuntimeUpdatesClassVisitor extends ClassVisitor {
        private Set<String> sourcePaths;
        private String classesPath;
        private String sourceFile;

        public RuntimeUpdatesClassVisitor(Set<String> sourcePaths, String classesPath) {
            super(Opcodes.ASM7);
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
