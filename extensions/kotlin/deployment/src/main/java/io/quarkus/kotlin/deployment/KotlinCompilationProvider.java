package io.quarkus.kotlin.deployment;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.config.Services;

import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.deployment.dev.CompilationProvider;

public class KotlinCompilationProvider implements CompilationProvider {

    private static final Logger log = Logger.getLogger(KotlinCompilationProvider.class);

    // see: https://github.com/JetBrains/kotlin/blob/v1.3.72/libraries/tools/kotlin-maven-plugin/src/main/java/org/jetbrains/kotlin/maven/KotlinCompileMojoBase.java#L181
    private final static Pattern OPTION_PATTERN = Pattern.compile("([^:]+):([^=]+)=(.*)");
    private static final String KOTLIN_PACKAGE = "org.jetbrains.kotlin";

    @Override
    public Set<String> handledExtensions() {
        return Collections.singleton(".kt");
    }

    @Override
    public void compile(Set<File> filesToCompile, Context context) {
        K2JVMCompilerArguments compilerArguments = new K2JVMCompilerArguments();
        compilerArguments.setJvmTarget(context.getTargetJvmVersion());
        compilerArguments.setJavaParameters(true);
        if (context.getCompilePluginArtifacts() != null && !context.getCompilePluginArtifacts().isEmpty()) {
            compilerArguments.setPluginClasspaths(context.getCompilePluginArtifacts().toArray(new String[0]));
        }
        if (context.getCompilerPluginOptions() != null && !context.getCompilerPluginOptions().isEmpty()) {
            List<String> sanitizedOptions = new ArrayList<>(context.getCompilerPluginOptions().size());
            for (String rawOption : context.getCompilerPluginOptions()) {
                Matcher matcher = OPTION_PATTERN.matcher(rawOption);
                if (!matcher.matches()) {
                    log.warn("Kotlin compiler plugin option " + rawOption + " is invalid");
                }

                String pluginId = matcher.group(1);
                if (!pluginId.contains(".")) {
                    // convert the plugin name to the plugin id by simply removing the dash and adding the kotlin package
                    // this seems to be the appropriate way of doing things for the plugins that were checked
                    pluginId = KOTLIN_PACKAGE + "." + pluginId.replace("-", "");
                }
                String key = matcher.group(2);
                String value = matcher.group(3);
                sanitizedOptions.add("plugin:" + pluginId + ":" + key + "=" + value);

                compilerArguments.setPluginOptions(sanitizedOptions.toArray(new String[0]));
            }
        }
        compilerArguments.setClasspath(
                context.getClasspath().stream().map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator)));
        compilerArguments.setDestination(context.getOutputDirectory().getAbsolutePath());
        compilerArguments.setFreeArgs(filesToCompile.stream().map(File::getAbsolutePath).collect(Collectors.toList()));

        compilerArguments.setSuppressWarnings(true);
        SimpleKotlinCompilerMessageCollector messageCollector = new SimpleKotlinCompilerMessageCollector();
        K2JVMCompiler compiler = new K2JVMCompiler();
        if (context.getCompilerOptions() != null && !context.getCompilerOptions().isEmpty()) {
            compiler.parseArguments(context.getCompilerOptions().toArray(new String[0]), compilerArguments);
        }

        ExitCode exitCode = compiler.exec(
                messageCollector,
                new Services.Builder().build(),
                compilerArguments);

        if (exitCode != ExitCode.OK && exitCode != ExitCode.COMPILATION_ERROR) {
            throw new RuntimeException("Unable to invoke Kotlin compiler. " + String.join("\n", messageCollector.getErrors()));
        }

        if (messageCollector.hasErrors()) {
            throw new RuntimeException("Compilation failed. " + String.join("\n", messageCollector.getErrors()));
        }
    }

    @Override
    public Path getSourcePath(Path classFilePath, PathsCollection sourcePaths, String classesPath) {
        // return same class so it is not removed
        return classFilePath;
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

        //kotlin 1.3 version
        public void report(CompilerMessageSeverity severity, String s, CompilerMessageLocation location) {
            if (severity.isError()) {
                if ((location != null) && (location.getLineContent() != null)) {
                    errors.add(String.format("%s%n%s:%d:%d%nReason: %s", location.getLineContent(), location.getPath(),
                            location.getLine(),
                            location.getColumn(), s));
                } else {
                    errors.add(s);
                }
            }
        }

        public List<String> getErrors() {
            return errors;
        }

        @Override
        public void report(@NotNull CompilerMessageSeverity severity, @NotNull String s,
                @Nullable CompilerMessageSourceLocation location) {
            if (severity.isError()) {
                if ((location != null) && (location.getLineContent() != null)) {
                    errors.add(String.format("%s%n%s:%d:%d%nReason: %s", location.getLineContent(), location.getPath(),
                            location.getLine(),
                            location.getColumn(), s));
                } else {
                    errors.add(s);
                }
            }
        }
    }
}
