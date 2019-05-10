package io.quarkus.dev;

import java.io.File;
import java.io.IOException;
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
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);) {

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
}
