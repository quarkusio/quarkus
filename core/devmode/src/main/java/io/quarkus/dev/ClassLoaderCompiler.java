package io.quarkus.dev;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.jboss.logging.Logger;

/**
 * Class that handles compilation of source files
 * 
 * @author Stuart Douglas
 */
public class ClassLoaderCompiler {

    private static final Logger log = Logger.getLogger(ClassLoaderCompiler.class);

    private final List<CompilationProvider> compilationProviders;
    /**
     * map of compilation contexts to source directories
     */
    private final Map<String, CompilationProvider.Context> compilationContexts = new HashMap<>();
    private final Set<String> allHandledExtensions;

    public ClassLoaderCompiler(ClassLoader classLoader,
            List<CompilationProvider> compilationProviders,
            DevModeContext context)
            throws IOException {
        this.compilationProviders = compilationProviders;

        Set<URL> urls = new HashSet<>();
        ClassLoader c = classLoader;
        while (c != null) {
            if (c instanceof URLClassLoader) {
                urls.addAll(Arrays.asList(((URLClassLoader) c).getURLs()));
            }
            c = c.getParent();
        }
        //this is pretty yuck, but under JDK11 the URLClassLoader trick does not work
        Enumeration<URL> manifests = classLoader.getResources("META-INF/MANIFEST.MF");
        while (manifests.hasMoreElements()) {
            URL url = manifests.nextElement();
            if (url.getProtocol().equals("jar")) {
                String path = url.getPath();
                if (path.startsWith("file:")) {
                    path = path.substring(5, path.lastIndexOf('!'));
                    urls.add(new File(URLDecoder.decode(path, StandardCharsets.UTF_8.name())).toURL());
                }
            }
        }

        urls.addAll(context.getClassPath());

        Set<String> parsedFiles = new HashSet<>();
        Deque<String> toParse = new ArrayDeque<>();
        for (URL url : urls) {
            toParse.add(new File(URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8.name())).getAbsolutePath());
        }
        Set<File> classPathElements = new HashSet<>();
        for (DevModeContext.ModuleInfo i : context.getModules()) {
            if (i.getClassesPath() != null) {
                classPathElements.add(new File(i.getClassesPath()));
            }
        }
        while (!toParse.isEmpty()) {
            String s = toParse.poll();
            if (!parsedFiles.contains(s)) {
                parsedFiles.add(s);
                File file = new File(s);
                if (!file.exists()) {
                    continue;
                }
                if (file.isDirectory()) {
                    classPathElements.add(file);
                } else if (file.getName().endsWith(".jar")) {
                    classPathElements.add(file);
                    if (!file.isDirectory() && file.getName().endsWith(".jar")) {
                        try (JarFile jar = new JarFile(file)) {
                            Manifest mf = jar.getManifest();
                            if (mf == null || mf.getMainAttributes() == null) {
                                continue;
                            }
                            Object classPath = mf.getMainAttributes().get(Attributes.Name.CLASS_PATH);
                            if (classPath != null) {
                                for (String i : classPath.toString().split(" ")) {
                                    File f;
                                    try {
                                        URL u = new URL(i);
                                        f = new File(u.getPath());
                                    } catch (MalformedURLException e) {
                                        f = new File(file.getParentFile(), i);
                                    }
                                    if (f.exists()) {
                                        toParse.add(f.getAbsolutePath());
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
        for (DevModeContext.ModuleInfo i : context.getModules()) {
            if (!i.getSourcePaths().isEmpty()) {
                if (i.getClassesPath() == null) {
                    log.warn("No classes directory found for module '" + i.getName()
                            + "'. It is advised that this module be compiled before launching dev mode");
                    continue;
                }
                i.getSourcePaths().forEach(sourcePath -> {
                    this.compilationContexts.put(sourcePath,
                            new CompilationProvider.Context(
                                    i.getName(),
                                    classPathElements,
                                    new File(i.getProjectDirectory()),
                                    new File(sourcePath),
                                    new File(i.getClassesPath()),
                                    context.getSourceEncoding()));
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
}
