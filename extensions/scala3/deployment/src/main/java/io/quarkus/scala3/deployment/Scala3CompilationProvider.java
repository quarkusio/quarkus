package io.quarkus.scala3.deployment;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import dotty.tools.dotc.interfaces.*;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.deployment.dev.CompilationProvider;

/**
 * Main.process() documentation for "dotty-interface" overload used here.
 * Architectural Decision Record, see javadoc comment below on why this particular appoach was used
 *
 * Notes:
 *   - This requires scala3-compiler in the dependencies and classpath of the consuming application
 *   - But it allows Quarkus to remain version-agnostic to Scala 3 compilation
 *       - We call the user's Scala 3 library to do the compiling
 *
 * Ref:
 *   - https://github.com/lampepfl/dotty/blob/b7d2a122555a6aa44cc7590852a80f12512c535e/compiler/src/dotty/tools/dotc/Driver.scala#L122-L145
 *   - https://github.com/lampepfl/dotty/blob/b7d2a122555a6aa44cc7590852a80f12512c535e/compiler/test/dotty/tools/dotc/InterfaceEntryPointTest.scala
 */

/** Entry point to the compiler that can be conveniently used with Java reflection.
 *
 *  This entry point can easily be used without depending on the `dotty` package,
 *  you only need to depend on `dotty-interfaces` and call this method using
 *  reflection. This allows you to write code that will work against multiple
 *  versions of dotty without recompilation.
 *
 *  The trade-off is that you can only pass a SimpleReporter to this method
 *  and not a normal Reporter which is more powerful.
 *
 *  Usage example: [[https://github.com/lampepfl/dotty/tree/master/compiler/test/dotty/tools/dotc/InterfaceEntryPointTest.scala]]
 *
 *  @param args       Arguments to pass to the compiler.
 *  @param simple     Used to log errors, warnings, and info messages.
 *                    The default reporter is used if this is `null`.
 *  @param callback   Used to execute custom code during the compilation
 *                    process. No callbacks will be executed if this is `null`.
 *  @return
 */

/**
 * This just here so that tooling doesn't try to attach the above Javadoc to this method
 */
public class Scala3CompilationProvider implements CompilationProvider {
    private final Logger log = Logger.getLogger(Scala3CompilationProvider.class);

    @Override
    public Set<String> handledExtensions() {
        return Collections.singleton(".scala");
    }

    @Override
    public void compile(Set<File> files, Context context) {
        List<String> sources = files.stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());

        String classpath = context.getClasspath().stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.joining(File.pathSeparator));

        List<String> dottyCompilerArgs = Arrays.asList(
                "-d", context.getOutputDirectory().getAbsolutePath(),
                "-classpath", classpath);

        String[] sourcesWithCompilerArgs = Stream
                .concat(sources.stream(), dottyCompilerArgs.stream())
                .toArray(String[]::new);

        CustomSimpleReporter reporter = new CustomSimpleReporter();
        CustomCompilerCallback callback = new CustomCompilerCallback();

        try {
            // Reflect to get the Dotty compiler on the application's Classpath
            Class<?> mainClass = Class.forName("dotty.tools.dotc.Main");
            Method process = mainClass.getMethod("process", String[].class, SimpleReporter.class, CompilerCallback.class);
            // Run the compiler by calling dotty.tools.dotc.Main.process
            process.invoke(null, sourcesWithCompilerArgs, reporter, callback);
        } catch (ClassNotFoundException e) {
            // Class.forName()
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // mainClass.getMethod()
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // process.invoke()
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // process.invoke()
            e.printStackTrace();
        }
    }

    @Override
    public Path getSourcePath(Path classFilePath, PathsCollection sourcePaths, String classesPath) {
        return classFilePath;
    }

    class CustomSimpleReporter implements SimpleReporter {
        Integer errorCount = 0;
        Integer warningCount = 0;

        /**
         * Report a diagnostic.
         *
         * @param diag the diagnostic message to report
         */
        @Override
        public void report(Diagnostic diag) {
            if (diag.level() == Diagnostic.ERROR) {
                errorCount += 1;
                log.error(diag.message());
            }
            if (diag.level() == Diagnostic.WARNING) {
                warningCount += 1;
                log.warn(diag.message());
            }
        }
    }

    // This is a no-op implementation right now, the super() calls invoke void methods
    // But it's useful for future reference I think
    class CustomCompilerCallback implements CompilerCallback {

        /**
         * Called when a class has been generated.
         *
         * @param source The source file corresponding to this class.
         *        Example: ./src/library/scala/collection/Seq.scala
         * @param generatedClass The generated classfile for this class.
         *        Example: ./scala/collection/Seq$.class
         * @param className The name of this class.
         */
        @Override
        public void onClassGenerated(SourceFile source, AbstractFile generatedClass, String className) {
            CompilerCallback.super.onClassGenerated(source, generatedClass, className);
        }

        /**
         * Called when every class for this file has been generated.
         *
         * @param source The source file.
         *        Example: ./src/library/scala/collection/Seq.scala
         */
        @Override
        public void onSourceCompiled(SourceFile source) {
            CompilerCallback.super.onSourceCompiled(source);
        }
    }
}
