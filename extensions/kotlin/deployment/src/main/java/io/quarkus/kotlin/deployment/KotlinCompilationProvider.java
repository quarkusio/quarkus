package io.quarkus.kotlin.deployment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.config.Services;

import io.quarkus.deployment.dev.CompilationProvider;

public class KotlinCompilationProvider implements CompilationProvider {

    private static final Logger log = Logger.getLogger(KotlinCompilationProvider.class);

    // see: https://github.com/JetBrains/kotlin/blob/v1.3.72/libraries/tools/kotlin-maven-plugin/src/main/java/org/jetbrains/kotlin/maven/KotlinCompileMojoBase.java#L181
    private final static Pattern OPTION_PATTERN = Pattern.compile("([^:]+):([^=]+)=(.*)");
    private static final String KOTLIN_PACKAGE = "org.jetbrains.kotlin";
    private static final String KOTLIN_PROVIDER_KEY = "kotlin";

    @Override
    public String getProviderKey() {
        return KOTLIN_PROVIDER_KEY;
    }

    @Override
    public Set<String> handledExtensions() {
        return Collections.singleton(".kt");
    }

    @Override
    public void compile(Set<File> filesToCompile, Context context) {
        final K2JVMCompilerArguments compilerArguments = new K2JVMCompilerArguments();
        compilerArguments.setJvmTarget(context.getTargetJvmVersion());
        compilerArguments.setJavaParameters(true);
        compilerArguments.setSuppressWarnings(true);

        if (context.getCompilePluginArtifacts() != null && !context.getCompilePluginArtifacts().isEmpty()) {
            compilerArguments.setPluginClasspaths(context.getCompilePluginArtifacts().toArray(new String[0]));
        }

        if (context.getCompilerPluginOptions() != null && !context.getCompilerPluginOptions().isEmpty()) {
            final List<String> sanitizedOptions = new ArrayList<>(context.getCompilerPluginOptions().size());
            for (String rawOption : context.getCompilerPluginOptions()) {
                final Matcher matcher = OPTION_PATTERN.matcher(rawOption);
                if (!matcher.matches()) {
                    log.warn("Kotlin compiler plugin option " + rawOption + " is invalid");
                }
                String pluginId = matcher.group(1);
                if (!pluginId.contains(".")) {
                    // convert the plugin name to the plugin id by simply removing the dash and adding the kotlin package
                    // this seems to be the appropriate way of doing things for the plugins that were checked
                    pluginId = KOTLIN_PACKAGE + "." + pluginId.replace("-", "");
                }
                final String key = matcher.group(2);
                final String value = matcher.group(3);
                sanitizedOptions.add("plugin:" + pluginId + ":" + key + "=" + value);
                compilerArguments.setPluginOptions(sanitizedOptions.toArray(new String[0]));
            }
        }

        final StringJoiner classpathJoiner = new StringJoiner(File.pathSeparator);
        context.getClasspath().forEach(file -> classpathJoiner.add(file.getAbsolutePath()));
        context.getReloadableClasspath().forEach(file -> classpathJoiner.add(file.getAbsolutePath()));

        compilerArguments.setClasspath(classpathJoiner.toString());
        compilerArguments.setFriendPaths(new String[] { context.getOutputDirectory().getAbsolutePath() });

        compilerArguments.setDestination(context.getOutputDirectory().getAbsolutePath());
        compilerArguments.setFreeArgs(filesToCompile.stream().map(File::getAbsolutePath).collect(Collectors.toList()));

        final K2JVMCompiler compiler = new K2JVMCompiler();
        final Collection<String> compilerOptions = context.getCompilerOptions(KOTLIN_PROVIDER_KEY);

        if (compilerOptions != null && !compilerOptions.isEmpty()) {
            compiler.parseArguments(compilerOptions.toArray(new String[0]), compilerArguments);
        }

        final SimpleKotlinCompilerMessageCollector messageCollector = new SimpleKotlinCompilerMessageCollector();
        final ExitCode exitCode = compiler.exec(messageCollector, new Services.Builder().build(), compilerArguments);

        if (exitCode != ExitCode.OK) {
            final String errors = String.join("\n", messageCollector.getErrors());

            if (exitCode != ExitCode.COMPILATION_ERROR) {
                throw new RuntimeException("Unable to invoke Kotlin compiler. " + errors);
            } else if (messageCollector.hasErrors()) {
                throw new RuntimeException("Compilation failed. " + errors);
            }
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

        public List<String> getErrors() {
            return errors;
        }

        @Override
        public void report(CompilerMessageSeverity severity, String s, CompilerMessageSourceLocation location) {
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
