package io.quarkus.deployment.dev;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.fs.util.FileSystemProviders;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathCollection;

/**
 * Class that handles compilation of source files
 *
 * @author Stuart Douglas
 */
public class QuarkusCompiler implements Closeable {

    private static final Logger log = Logger.getLogger(QuarkusCompiler.class);
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile(" ");

    private final List<CompilationProvider> compilationProviders;
    /**
     * map of compilation contexts to source directories
     */
    private final Map<String, CompilationProvider.Context> compilationContexts = new HashMap<>();
    private final Set<String> allHandledExtensions;

    public QuarkusCompiler(CuratedApplication application,
            List<CompilationProvider> compilationProviders,
            DevModeContext context) throws IOException {
        this.compilationProviders = compilationProviders;

        Set<File> classPathElements = new HashSet<>();
        for (DevModeContext.ModuleInfo i : context.getAllModules()) {
            if (i.getMain().getClassesPath() != null) {
                classPathElements.add(new File(i.getMain().getClassesPath()));
            }
            if (application.getQuarkusBootstrap().getMode() == QuarkusBootstrap.Mode.TEST) {
                if (i.getTest().isPresent()) {
                    classPathElements.add(new File(i.getTest().get().getClassesPath()));
                }
            }
        }

        final Set<Path> paths = new HashSet<>();
        final Set<Path> reloadablePaths = new HashSet<>(0);

        final boolean skipReloadableArtifacts = !application.hasReloadableArtifacts();

        for (ResolvedDependency i : application.getApplicationModel().getRuntimeDependencies()) {
            if (skipReloadableArtifacts) {
                paths.addAll(i.getContentTree().getRoots());
            } else {
                if (application.isReloadableArtifact(i.getKey())) {
                    reloadablePaths.addAll(i.getContentTree().getRoots());
                } else {
                    paths.addAll(i.getContentTree().getRoots());
                }
            }
        }

        final String devModeRunnerJarCanonicalPath = context.getDevModeRunnerJarFile() == null
                ? null
                : context.getDevModeRunnerJarFile().getCanonicalPath();

        final Set<String> parsedFiles = new HashSet<>();
        parseClassPath(devModeRunnerJarCanonicalPath, classPathElements, new ArrayDeque<>(paths), parsedFiles);

        final Set<File> reloadableClassPathElements;
        if (reloadablePaths.isEmpty()) {
            reloadableClassPathElements = Set.of();
        } else {
            reloadableClassPathElements = new HashSet<>(reloadablePaths.size());
            parseClassPath(devModeRunnerJarCanonicalPath,
                    reloadableClassPathElements, new ArrayDeque<>(reloadablePaths), parsedFiles);
        }

        for (DevModeContext.ModuleInfo module : context.getAllModules()) {
            setupSourceCompilationContext(context, classPathElements, reloadableClassPathElements,
                    module, module.getMain(), "classes");
            if (application.getQuarkusBootstrap().getMode() == QuarkusBootstrap.Mode.TEST && module.getTest().isPresent()) {
                setupSourceCompilationContext(context, classPathElements, reloadableClassPathElements,
                        module, module.getTest().get(), "test classes");
            }
        }
        this.allHandledExtensions = new HashSet<>();
        for (CompilationProvider compilationProvider : compilationProviders) {
            this.allHandledExtensions.addAll(compilationProvider.handledExtensions());
        }
    }

    private void parseClassPath(final String devModeRunnerJarCanonicalPath,
            final Set<File> classPathElements,
            final Deque<Path> toParse,
            final Set<String> parsedFiles) throws IOException {
        while (!toParse.isEmpty()) {
            Path path = toParse.poll();
            File file = path.toFile();
            String s = file.getAbsolutePath();
            if (!parsedFiles.contains(s)) {
                parsedFiles.add(s);
                if (!file.exists()) {
                    continue;
                }
                if (path.getFileSystem() == FileSystems.getDefault()) {
                    classPathElements.add(file);
                } else if (path.getFileSystem().provider() == FileSystemProviders.ZIP_PROVIDER) {
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
                        log.debug("Dev mode runner jar " + file + " won't be added to compilation classpath of hot deployment");
                    } else {
                        classPathElements.add(file);
                    }

                    try (JarFile jar = new JarFile(file)) {
                        Manifest mf = jar.getManifest();
                        if (mf == null || mf.getMainAttributes() == null) {
                            continue;
                        }
                        Object classPath = mf.getMainAttributes().get(Attributes.Name.CLASS_PATH);
                        if (classPath != null) {
                            for (String classPathEntry : WHITESPACE_PATTERN.split(classPath.toString())) {
                                final URI cpEntryURI = new URI(classPathEntry);
                                File f;
                                // if it's a "file" scheme URI, then use the path as a file system path
                                // without the need to resolve it
                                if (cpEntryURI.isAbsolute() && cpEntryURI.getScheme().equals("file")) {
                                    f = new File(cpEntryURI.getPath());
                                } else {
                                    try {
                                        f = Paths.get(new URI("file", null, "/", null).resolve(cpEntryURI)).toFile();
                                    } catch (URISyntaxException e) {
                                        f = new File(file.getParentFile(), classPathEntry);
                                    }
                                }
                                if (f.exists()) {
                                    toParse.add(f.toPath());
                                }
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to open class path file " + file, e);
                    }
                }
            }
        }
    }

    public void setupSourceCompilationContext(DevModeContext context,
            Set<File> classPathElements,
            Set<File> reloadableClassPathElements,
            DevModeContext.ModuleInfo i,
            DevModeContext.CompilationUnit compilationUnit, String name) {
        if (!compilationUnit.getSourcePaths().isEmpty()) {
            if (compilationUnit.getClassesPath() == null) {
                log.warn("No " + name + " directory found for module '" + i.getName()
                        + "'. It is advised that this module be compiled before launching dev mode");
                return;
            }

            for (Path sourcePath : compilationUnit.getSourcePaths()) {
                final String srcPathStr = sourcePath.toString();
                if (this.compilationContexts.containsKey(srcPathStr)) {
                    continue;
                }

                this.compilationContexts.put(srcPathStr,
                        new CompilationProvider.Context(
                                i.getName(),
                                classPathElements,
                                reloadableClassPathElements,
                                i.getProjectDirectory() == null ? null : new File(i.getProjectDirectory()),
                                sourcePath.toFile(),
                                new File(compilationUnit.getClassesPath()),
                                context.getSourceEncoding(),
                                context.getCompilerOptions(),
                                context.getReleaseJavaVersion(),
                                context.getSourceJavaVersion(),
                                context.getTargetJvmVersion(),
                                context.getCompilerPluginArtifacts(),
                                context.getCompilerPluginsOptions(),
                                compilationUnit.getGeneratedSourcesPath() == null ? null
                                        : new File(compilationUnit.getGeneratedSourcesPath()),
                                context.getAnnotationProcessorPaths(),
                                context.getAnnotationProcessors(),
                                context.getBuildSystemProperties().getOrDefault("quarkus.live-reload.ignore-module-info",
                                        "true")));
            }
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

    public Path findSourcePath(Path classFilePath, PathCollection sourcePaths, String classesPath) {
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
