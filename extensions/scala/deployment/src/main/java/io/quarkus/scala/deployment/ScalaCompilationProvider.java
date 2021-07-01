package io.quarkus.scala.deployment;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.deployment.dev.CompilationProvider;
import scala.collection.JavaConverters;

import scala.tools.nsc.Global;
import scala.tools.nsc.Settings;

import dotty.tools.dotc.core.Contexts.ContextBase;
import dotty.tools.dotc.interfaces.CompilerCallback;
import dotty.tools.dotc.Main;
import dotty.tools.dotc.core.Contexts;
import dotty.tools.dotc.interfaces.SourceFile;
import dotty.tools.dotc.reporting.Diagnostic;
import dotty.tools.dotc.reporting.Reporter;

public class ScalaCompilationProvider implements CompilationProvider {
    @Override
    public Set<String> handledExtensions() {
        return Collections.singleton(".scala");
    }

    @Override
    public void compile(Set<File> files, Context context) {
        Settings settings = new Settings();
        context.getClasspath().stream()
                .map(File::getAbsolutePath)
                .forEach(f -> settings.classpath().append(f));
        settings.outputDirs().add(context.getSourceDirectory().getAbsolutePath(),
                context.getOutputDirectory().getAbsolutePath());
        try (Global g = new Global(settings)) {
            Global.Run run = g.new Run();
            Set<String> fileSet = files.stream()
                    .map(File::getAbsolutePath)
                    .collect(Collectors.toSet());
            run.compile(JavaConverters.asScalaSet(fileSet).toList());
        }
    }

    @Override
    public Path getSourcePath(Path classFilePath, PathsCollection sourcePaths, String classesPath) {
        return classFilePath;
    }
}


class Scala3CompilationProvider implements CompilationProvider {

    @Override
    public Set<String> handledExtensions() {
        return Collections.singleton(".scala");
    }

    // TODO: What does this even do? The implementation has no docs, and it doesn't
    // seem to exist on the Kotlin or Scala extensions.
    @Override
    public Set<String> handledSourcePaths() {
        return CompilationProvider.super.handledSourcePaths();
    }

    @Override
    public void compile(Set<File> files, Context context) {
        var reporter = new CustomReporter();
        var callback = new CustomCompilerCallback();

        var classPath = context.getClasspath().stream().map(File::getAbsolutePath)
                .collect(Collectors.joining(File.pathSeparator));

        var scalaCtx = new ContextBase().initialCtx().fresh().setReporter(reporter).setCompilerCallback(callback);
        scalaCtx.setSetting(scalaCtx.settings().classpath(), classPath);
        scalaCtx.setSetting(scalaCtx.settings().outputDir(), context.getOutputDirectory().getAbsolutePath());

        var args = new String[] {};
        Main.process(args, scalaCtx);
    }

    @Override
    public Path getSourcePath(Path classFilePath, PathsCollection sourcePaths, String classesPath) {
        return classFilePath;
    }

    @Override
    public void close() throws IOException {
        CompilationProvider.super.close();
    }

    private class CustomReporter extends Reporter
    // with UniqueMessagePositions
    // with HideNonSensicalMessages
    {
        @Override
        public void doReport(Diagnostic message, Contexts.Context ctx) {
            //
        }
    }

    private class CustomCompilerCallback implements CompilerCallback {
        private final ArrayList<String> pathsList = new ArrayList<>();

        public ArrayList<String> getPathsList() {
            return pathsList;
        }

        @Override
        public void onSourceCompiled(SourceFile source) {
            if (source.jfile().isPresent())
                pathsList.add(source.jfile().get().getPath());
        }
    }
}
