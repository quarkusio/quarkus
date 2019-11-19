package io.quarkus.runner;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.MessageDigest;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import io.quarkus.deployment.ClassOutput;
import io.quarkus.deployment.QuarkusClassWriter;

public class RuntimeClassLoader extends ClassLoader implements ClassOutput, TransformerTarget {

    private static final Logger log = Logger.getLogger(RuntimeClassLoader.class);

    private final Map<String, byte[]> appClasses = new ConcurrentHashMap<>();
    private final Set<String> frameworkClasses = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final Map<String, byte[]> resources = new ConcurrentHashMap<>();

    private volatile Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> bytecodeTransformers = null;

    private final List<Path> applicationClassDirectories;

    private final Map<String, Path> applicationClasses;

    private final ProtectionDomain defaultProtectionDomain;

    private final Path frameworkClassesPath;
    private final Path transformerCache;

    private static final String DEBUG_CLASSES_DIR = System.getProperty("quarkus.debug.generated-classes-dir");

    private final ConcurrentHashMap<String, Future<Class<?>>> loadingClasses = new ConcurrentHashMap<>();

    static {
        registerAsParallelCapable();
    }

    public RuntimeClassLoader(ClassLoader parent, List<Path> applicationClassesDirectories, Path frameworkClassesDirectory,
            Path transformerCache) {
        super(parent);
        try {
            Map<String, Path> applicationClasses = new HashMap<>();
            for (Path i : applicationClassesDirectories) {
                if (Files.isDirectory(i)) {
                    try (Stream<Path> fileTreeElements = Files.walk(i)) {
                        fileTreeElements.forEach(new Consumer<Path>() {
                            @Override
                            public void accept(Path path) {
                                if (path.toString().endsWith(".class")) {
                                    applicationClasses.put(i.relativize(path).toString().replace('\\', '/'), path);
                                }
                            }
                        });
                    }
                }
            }

            this.defaultProtectionDomain = createDefaultProtectionDomain(applicationClassesDirectories.get(0));
            this.applicationClasses = applicationClasses;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.applicationClassDirectories = applicationClassesDirectories;
        this.frameworkClassesPath = frameworkClassesDirectory;
        if (!Files.isDirectory(frameworkClassesDirectory)) {
            throw new IllegalStateException(
                    "Test classes directory path does not point to an existing directory: " + frameworkClassesPath);
        }
        this.transformerCache = transformerCache;
    }

    @Override
    public Enumeration<URL> getResources(String nm) throws IOException {
        String name = sanitizeName(nm);

        List<URL> resources = new ArrayList<>();

        // TODO: some superugly hack for bean provider
        URL resource = getQuarkusResource(name);
        if (resource != null) {
            resources.add(resource);
        }

        URL appResource = findApplicationResource(name);
        if (appResource != null) {
            resources.add(appResource);
        }

        for (Enumeration<URL> e = super.getResources(name); e.hasMoreElements();) {
            resources.add(e.nextElement());
        }

        return Collections.enumeration(resources);
    }

    @Override
    public URL getResource(String nm) {
        String name = sanitizeName(nm);

        // TODO: some superugly hack for bean provider
        URL resource = getQuarkusResource(name);
        if (resource != null) {
            return resource;
        }

        URL appResource = findApplicationResource(name);
        if (appResource != null) {
            return appResource;
        }
        return super.getResource(name);
    }

    @Override
    public InputStream getResourceAsStream(String nm) {
        String name = sanitizeName(nm);

        byte[] data = resources.get(name);
        if (data != null) {
            return new ByteArrayInputStream(data);
        }

        data = findApplicationResourceContent(name);
        if (data != null) {
            return new ByteArrayInputStream(data);
        }

        return super.getResourceAsStream(name);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> ex = findLoadedClass(name);
        if (ex != null) {
            return ex;
        }

        if (appClasses.containsKey(name)
                || (!frameworkClasses.contains(name) && getClassInApplicationClassPaths(name) != null)) {
            return findClass(name);
        }

        return super.loadClass(name, resolve);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> existing = findLoadedClass(name);
        if (existing != null) {
            return existing;
        }

        byte[] bytes = appClasses.get(name);
        if (bytes != null) {
            try {
                definePackage(name);
                return defineClass(name, bytes, 0, bytes.length, defaultProtectionDomain);
            } catch (Error e) {
                //potential race conditions if another thread is loading the same class
                existing = findLoadedClass(name);
                if (existing != null) {
                    return existing;
                }
                throw e;
            }
        }

        Path classLoc = getClassInApplicationClassPaths(name);

        if (classLoc != null) {
            CompletableFuture<Class<?>> res = new CompletableFuture<>();
            Future<Class<?>> loadingClass = loadingClasses.putIfAbsent(name, res);
            if (loadingClass != null) {
                try {
                    return loadingClass.get();
                } catch (Exception e) {
                    throw new ClassNotFoundException("Failed to load " + name, e);
                }
            }
            try {
                try {
                    bytes = Files.readAllBytes(classLoc);
                } catch (IOException e) {
                    throw new ClassNotFoundException("Failed to load class", e);
                }
                bytes = handleTransform(name, bytes);
                definePackage(name);
                Class<?> clazz = defineClass(name, bytes, 0, bytes.length, defaultProtectionDomain);
                res.complete(clazz);
                return clazz;
            } catch (RuntimeException e) {
                res.completeExceptionally(e);
                throw e;
            } catch (Throwable e) {
                res.completeExceptionally(e);
                throw e;
            }
        }

        throw new ClassNotFoundException(name);
    }

    @Override
    public void writeClass(boolean applicationClass, String className, byte[] data) {
        if (applicationClass) {
            String dotName = className.replace('/', '.');
            appClasses.put(dotName, data);
            if (DEBUG_CLASSES_DIR != null) {
                try {
                    File debugPath = new File(DEBUG_CLASSES_DIR);
                    if (!debugPath.exists()) {
                        debugPath.mkdir();
                    }
                    File classFile = new File(debugPath, dotName + ".class");
                    try (FileOutputStream classWriter = new FileOutputStream(classFile)) {
                        classWriter.write(data);
                    }
                    log.infof("Wrote %s", classFile.getAbsolutePath());
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        } else {
            //this is pretty horrible
            //basically we add the framework level classes to the file system
            //in the same dir as the actual app classes
            //however as we add them to the frameworkClasses set we know to load them
            //from the parent CL
            frameworkClasses.add(className.replace('/', '.'));
            final Path fileName = frameworkClassesPath.resolve(className.replace('.', '/') + ".class");
            try {
                Files.createDirectories(fileName.getParent());
                try (FileOutputStream out = new FileOutputStream(fileName.toFile())) {
                    out.write(data);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void setTransformers(Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> functions) {
        this.bytecodeTransformers = functions;
    }

    public void setApplicationArchives(List<Path> archives) {
        //we also need to be able to transform application archives
        //this is not great but I can't really see a better solution
        if (bytecodeTransformers == null) {
            return;
        }
        try {
            for (Path root : archives) {
                Map<String, Path> classes = new HashMap<>();
                AtomicBoolean transform = new AtomicBoolean();
                try (Stream<Path> fileTreeElements = Files.walk(root)) {
                    fileTreeElements.forEach(new Consumer<Path>() {
                        @Override
                        public void accept(Path path) {
                            if (path.toString().endsWith(".class")) {
                                String key = root.relativize(path).toString().replace('\\', '/');
                                classes.put(key, path);
                                if (bytecodeTransformers
                                        .containsKey(key.substring(0, key.length() - ".class".length()).replace("/", "."))) {
                                    transform.set(true);
                                }
                            }
                        }
                    });
                }
                if (transform.get()) {
                    applicationClasses.putAll(classes);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeResource(String name, byte[] data) throws IOException {
        resources.put(name, data);
    }

    /**
     * This is needed in order to easily inject classes into the classloader
     * without having to resort to tricks (that don't work that well on new JDKs)
     * See {@link io.quarkus.deployment.proxy.InjectIntoClassloaderClassOutput}
     */
    public Class<?> visibleDefineClass(String name, byte[] b, int off, int len) throws ClassFormatError {
        return super.defineClass(name, b, off, len, defaultProtectionDomain);
    }

    private void definePackage(String name) {
        final String pkgName = getPackageNameFromClassName(name);
        if ((pkgName != null) && getPackage(pkgName) == null) {
            synchronized (getClassLoadingLock(pkgName)) {
                if (getPackage(pkgName) == null) {
                    // this could certainly be improved to use the actual manifest
                    definePackage(pkgName, null, null, null, null, null, null, null);
                }
            }
        }
    }

    private String getPackageNameFromClassName(String className) {
        final int index = className.lastIndexOf('.');
        if (index == -1) {
            // we return null here since in this case no package is defined
            // this is same behavior as Package.getPackage(clazz) exhibits
            // when the class is in the default package
            return null;
        }
        return className.substring(0, index);
    }

    private static byte[] readFileContent(final Path path) {
        final File file = path.toFile();
        final long fileLength = file.length();
        if (fileLength > Integer.MAX_VALUE) {
            throw new RuntimeException("Can't process class files larger than Integer.MAX_VALUE bytes");
        }
        final int intLength = (int) fileLength;
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            //Might be large but we need a single byte[] at the end of things, might as well allocate it in one shot:
            ByteArrayOutputStream out = new ByteArrayOutputStream(intLength);
            final int reasonableBufferSize = Math.min(intLength, 2048);
            byte[] buf = new byte[reasonableBufferSize];
            int r;
            while ((r = in.read(buf)) > 0) {
                out.write(buf, 0, r);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read file " + path, e);
        }
    }

    private byte[] handleTransform(String name, byte[] bytes) {
        if (bytecodeTransformers == null || bytecodeTransformers.isEmpty()) {
            return bytes;
        }
        List<BiFunction<String, ClassVisitor, ClassVisitor>> transformers = bytecodeTransformers.get(name);
        if (transformers == null) {
            return bytes;
        }

        Path hashPath = null;
        if (transformerCache != null) {

            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] thedigest = md.digest(bytes);
                String hash = Base64.getUrlEncoder().encodeToString(thedigest);
                hashPath = transformerCache.resolve(hash);
                if (Files.exists(hashPath)) {
                    return readFileContent(hashPath);
                }
            } catch (Exception e) {
                log.error("Unable to load transformed class from cache", e);
            }
        }

        ClassReader cr = new ClassReader(bytes);
        ClassWriter writer = new QuarkusClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = writer;
        for (BiFunction<String, ClassVisitor, ClassVisitor> i : transformers) {
            visitor = i.apply(name, visitor);
        }
        cr.accept(visitor, 0);
        byte[] data = writer.toByteArray();
        if (hashPath != null) {
            try {

                File file = hashPath.toFile();
                file.getParentFile().mkdirs();
                try (FileOutputStream out = new FileOutputStream(file)) {
                    out.write(data);
                }
            } catch (Exception e) {
                log.error("Unable to write class to cache", e);
            }
        }
        return data;
    }

    private String sanitizeName(String name) {
        if (name.startsWith("/")) {
            return name.substring(1);
        }

        return name;
    }

    private Path getClassInApplicationClassPaths(String name) {
        final String fileName = name.replace('.', '/') + ".class";
        return applicationClasses.get(fileName);
    }

    private URL findApplicationResource(String name) {
        Path resourcePath = null;

        for (Path i : applicationClassDirectories) {
            resourcePath = i.resolve(name);
            if (Files.exists(resourcePath)) {
                break;
            }
        }
        try {
            return resourcePath != null && Files.exists(resourcePath) ? resourcePath.toUri()
                    .toURL() : null;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] findApplicationResourceContent(String name) {
        Path resourcePath = null;

        for (Path i : applicationClassDirectories) {
            resourcePath = i.resolve(name);
            if (Files.exists(resourcePath)) {
                return readFileContent(resourcePath);
            }
        }

        return null;
    }

    private URL getQuarkusResource(String name) {
        byte[] data = resources.get(name);
        if (data != null) {
            String path = "quarkus:" + name;

            try {
                URL url = new URL(null, path, new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(final URL u) throws IOException {
                        return new URLConnection(u) {
                            @Override
                            public void connect() throws IOException {
                            }

                            @Override
                            public InputStream getInputStream() throws IOException {
                                return new ByteArrayInputStream(resources.get(name));
                            }
                        };
                    }
                });

                return url;
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Invalid URL: " + path);
            }
        }
        return null;
    }

    private ProtectionDomain createDefaultProtectionDomain(Path applicationClasspath) {
        URL url = null;
        if (applicationClasspath != null) {
            try {
                URI uri = new URI("file", null, applicationClasspath.toString(), null);
                url = uri.toURL();
            } catch (URISyntaxException | MalformedURLException e) {
                log.error("URL codeSource location for path " + applicationClasspath + " could not be created.");
            }
        }
        CodeSource codesource = new CodeSource(url, (Certificate[]) null);
        ProtectionDomain protectionDomain = new ProtectionDomain(codesource, null, this, null);
        return protectionDomain;
    }
}
