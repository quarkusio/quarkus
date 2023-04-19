package io.quarkus.groovy.deployment;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.jboss.logging.Logger;

import groovy.lang.GroovyClassLoader;
import io.quarkus.deployment.dev.CompilationProvider;
import io.quarkus.paths.PathCollection;

public class GroovyCompilationProvider implements CompilationProvider {

    private static final Logger log = Logger.getLogger(GroovyCompilationProvider.class);
    private static final String GROOVY_PROVIDER_KEY = "groovy";
    private static final Pattern OPTION_PATTERN = Pattern.compile("([^=]+)=(.*)");

    @Override
    public String getProviderKey() {
        return GROOVY_PROVIDER_KEY;
    }

    @Override
    public Set<String> handledExtensions() {
        return Set.of(".groovy");
    }

    @Override
    public void compile(Set<File> filesToCompile, Context context) {
        CompilerConfiguration cc = getCompilerConfiguration(context);
        cc.setSourceEncoding(context.getSourceEncoding().name());
        cc.setTargetBytecode(context.getTargetJvmVersion());
        cc.setTargetDirectory(context.getOutputDirectory().getAbsolutePath());
        cc.setClasspathList(
                Stream.of(context.getClasspath(), context.getReloadableClasspath())
                        .flatMap(Collection::stream)
                        .map(File::getAbsolutePath).collect(Collectors.toList()));
        try (GroovyClassLoader cl = new GroovyClassLoader(ClassLoader.getSystemClassLoader(), cc, true)) {
            CompilationUnit unit = new CompilationUnit(cc, null, cl);
            filesToCompile.forEach(unit::addSource);
            unit.compile();
        } catch (CompilationFailedException e) {
            // Convert the CompilationFailedException into a RuntimeException to prevent serialization issues in remote
            // dev mode
            throw new RuntimeException(e.getMessage());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static CompilerConfiguration getCompilerConfiguration(Context context) {
        final Collection<String> compilerOptions = context.getCompilerOptions(GROOVY_PROVIDER_KEY);
        CompilerConfiguration cc;
        if (compilerOptions != null && !compilerOptions.isEmpty()) {
            Properties properties = new Properties(compilerOptions.size());
            for (String rawOption : compilerOptions) {
                final Matcher matcher = OPTION_PATTERN.matcher(rawOption);
                if (!matcher.matches()) {
                    log.warnf("Groovy compiler option %s is invalid", rawOption);
                }
                properties.setProperty(matcher.group(1), matcher.group(2));
            }
            cc = new CompilerConfiguration(properties);
        } else {
            cc = new CompilerConfiguration();
        }
        return cc;
    }

    @Override
    public Path getSourcePath(Path classFilePath, PathCollection sourcePaths, String classesPath) {
        // return same class so it is not removed
        return classFilePath;
    }
}
