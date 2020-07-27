package io.quarkus.deployment.dev;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;

/**
 * Class that handles compilation of source files
 *
 * @author Stuart Douglas
 */
public class DevModeCompiler implements Closeable {

    private static final Logger log = Logger.getLogger(DevModeCompiler.class);
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile(" ");

    private final List<CompilationProvider> compilationProviders;
    /**
     * map of compilation contexts to source directories
     */
    private final Map<String, CompilationProvider.Context> compilationContexts = new HashMap<>();
    private final Set<String> allHandledExtensions;

    public DevModeCompiler(CuratedApplication application,
            List<CompilationProvider> compilationProviders,
            DevModeContext context)
            throws IOException {
        this.compilationProviders = compilationProviders;

        for (DevModeContext.ModuleInfo i : context.getAllModules()) {
            if (!i.getSourcePaths().isEmpty()) {
                if (i.getClassesPath() == null) {
                    log.warn("No classes directory found for module '" + i.getName()
                            + "'. It is advised that this module be compiled before launching dev mode");
                    continue;
                }

                Set<URL> urls = new HashSet<>();
                List<AppArtifact> dependencies = i.getDependencies();
                if (dependencies == null) {
                    dependencies = application.getAppModel().getUserDependencies().stream().map(AppDependency::getArtifact)
                            .collect(Collectors.toList());
                }
                for (AppArtifact ad : dependencies) {
                    for (Path p : ad.getPaths()) {
                        urls.add(p.toUri().toURL());
                    }
                }

                Set<String> parsedFiles = new HashSet<>();
                List<String> toParse = new ArrayList<>();
                for (URL url : urls) {
                    toParse.add(new File(URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8.name())).getAbsolutePath());
                }
                Set<File> classPathElements = new HashSet<>();
                if (i.getClassesPath() != null) {
                    classPathElements.add(new File(i.getClassesPath()));
                }
                final String devModeRunnerJarCanonicalPath = context.getDevModeRunnerJarFile() == null
                        ? null
                        : context.getDevModeRunnerJarFile().getCanonicalPath();
                for (String s : toParse) {
                    if (!parsedFiles.contains(s)) {
                        parsedFiles.add(s);
                        File file = new File(s);
                        if (!file.exists()) {
                            continue;
                        }
                        if (file.isDirectory()) {
                            classPathElements.add(file);
                        } else if (file.getName().endsWith(".jar")) {
                            // skip adding the dev mode runner jar to the classpath to prevent
                            // hitting a bug in JDK - https://bugs.openjdk.java.net/browse/JDK-8232170
                            // which causes the programmatic java file compilation to fail.
                            // see details in https://github.com/quarkusio/quarkus/issues/3592.
                            // we anyway don't need to add that jar to the hot deployment classpath since the
                            // current running JVM is already launched using that jar, plus it doesn't
                            // have any application resources/classes. The Class-Path jar(s) contained
                            // in the MANIFEST.MF of that dev mode runner jar are anyway added explicitly
                            // in various different ways in this very own ClassLoaderCompiler class, so
                            // not passing this jar to the JDK's compiler won't prevent its Class-Path
                            // references from being part of the hot deployment compile classpath.
                            if (devModeRunnerJarCanonicalPath != null
                                    && file.getCanonicalPath().equals(devModeRunnerJarCanonicalPath)) {
                                log.debug("Dev mode runner jar " + file
                                        + " won't be added to compilation classpath of hot deployment");
                            } else {
                                classPathElements.add(file);
                            }
                        }
                    }
                }

                i.getSourcePaths().forEach(sourcePath -> {
                    this.compilationContexts.put(sourcePath,
                            new CompilationProvider.Context(
                                    i.getName(),
                                    classPathElements,
                                    new File(i.getProjectDirectory()),
                                    new File(sourcePath),
                                    new File(i.getClassesPath()),
                                    context.getSourceEncoding(),
                                    context.getCompilerOptions(),
                                    context.getSourceJavaVersion(),
                                    context.getTargetJvmVersion(),
                                    context.getCompilerPluginArtifacts(),
                                    context.getCompilerPluginsOptions()));
                });
            }
        }
        this.allHandledExtensions = new HashSet<>();
        for (CompilationProvider compilationProvider : compilationProviders) {
            allHandledExtensions.addAll(compilationProvider.handledExtensions());
        }
    }

    public Set<String> allHandledExtensions() {
        return allHandledExtensions;
    }

    public void compile(String sourceDir, Map<String, Set<File>> extensionToChangedFiles) {
        CompilationProvider.Context compilationContext = compilationContexts.get(sourceDir);
        for (String extension : extensionToChangedFiles.keySet()) {
            for (CompilationProvider compilationProvider : compilationProviders) {
                if (compilationProvider.handledExtensions().contains(extension)) {
                    compilationProvider.compile(extensionToChangedFiles.get(extension), compilationContext);
                    break;
                }
            }
        }
    }

    public Path findSourcePath(Path classFilePath, Set<String> sourcePaths, String classesPath) {
        for (CompilationProvider compilationProvider : compilationProviders) {
            Path sourcePath = compilationProvider.getSourcePath(classFilePath, sourcePaths, classesPath);

            if (sourcePath != null) {
                return sourcePath;
            }
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        for (CompilationProvider i : compilationProviders) {
            i.close();
        }
    }
}
