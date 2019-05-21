package io.quarkus.kotlin.deployment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.config.Services;

import io.quarkus.dev.CompilationProvider;

public class KotlinCompilationProvider implements CompilationProvider {

    @Override
    public Set<String> handledExtensions() {
        return Collections.singleton(".kt");
    }

    @Override
    public void compile(Set<File> filesToCompile, Context context) {
        K2JVMCompilerArguments compilerArguments = new K2JVMCompilerArguments();
        compilerArguments.setClasspath(
                context.getClasspath().stream().map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator)));
        compilerArguments.setDestination(context.getOutputDirectory().getAbsolutePath());
        compilerArguments.setFreeArgs(filesToCompile.stream().map(File::getAbsolutePath).collect(Collectors.toList()));
        compilerArguments.setSuppressWarnings(true);
        SimpleKotlinCompilerMessageCollector messageCollector = new SimpleKotlinCompilerMessageCollector();
        ExitCode exitCode = new K2JVMCompiler().exec(
                messageCollector,
                new Services.Builder().build(),
                compilerArguments);

        if (exitCode != ExitCode.OK && exitCode != ExitCode.COMPILATION_ERROR) {
            throw new RuntimeException("Unable to invoke Kotlin compiler");
        }

        if (messageCollector.hasErrors()) {
            throw new RuntimeException("Compilation failed" + String.join("\n", messageCollector.getErrors()));
        }
    }

    private static class SimpleKotlinCompilerMessageCollector implements MessageCollector {

        private final List<String> errors = new ArrayList<>();

        @Override
        public void clear() {
        }

        @Override
        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        @Override
        public void report(CompilerMessageSeverity severity, String s, CompilerMessageLocation location) {
            if (severity.isError()) {
                if ((location != null) && (location.getLineContent() != null)) {
                    errors.add(String.format("%s\n%s:%d:%d", location.getLineContent(), location.getPath(), location.getLine(),
                            location.getColumn()));
                } else {
                    errors.add(s);
                }
            }
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
