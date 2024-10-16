package io.quarkus.deployment.dev.filesystem;

import static io.quarkus.commons.classloading.ClassLoaderHelper.fromClassNameToResourceName;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Vector;
import java.util.function.Supplier;

import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

import io.quarkus.runtime.util.ClassPathUtils;

/**
 * This reloadable file manager handle the class-paths and file locations of separated file manager instances. <br>
 * It is required for applications that depends on hot-deployed classes of external sources.
 *
 * @see io.quarkus.deployment.configuration.ClassLoadingConfig#reloadableArtifacts
 *      Quarkus Class Loading Reference
 */
public class ReloadableFileManager extends QuarkusFileManager {

    private final StandardJavaFileManager reloadableFileManager;

    public ReloadableFileManager(Supplier<StandardJavaFileManager> supplier, Context context) {
        this(supplier.get(), supplier.get(), context);
    }

    protected ReloadableFileManager(StandardJavaFileManager fileManager,
            StandardJavaFileManager reloadableFileManager, Context context) {
        super(fileManager, context);
        this.reloadableFileManager = reloadableFileManager;
        try {
            this.reloadableFileManager.setLocation(StandardLocation.CLASS_PATH, context.getReloadableClassPath());
        } catch (IOException e) {
            throw new RuntimeException("Cannot initialize reloadable file manager", e);
        }
    }

    @Override
    public ClassLoader getClassLoader(Location location) {
        if (this.reloadableFileManager == null) {
            return super.getClassLoader(location);
        }
        final ClassLoader staticClassLoader = super.getClassLoader(location);
        if (staticClassLoader == null) {
            return this.reloadableFileManager.getClassLoader(location);
        }
        final ClassLoader reloadableClassLoader = this.reloadableFileManager.getClassLoader(location);
        if (reloadableClassLoader == null) {
            return staticClassLoader;
        }
        return new JoinClassLoader(staticClassLoader.getParent(), staticClassLoader, reloadableClassLoader);
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName,
            Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
        return combinedIterable(() -> {
            try {
                return super.list(location, packageName, kinds, recurse);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, this.reloadableFileManager == null ? null : () -> {
            try {
                return this.reloadableFileManager.list(location, packageName, kinds, recurse);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        String result = super.inferBinaryName(location, file);
        if (result == null && this.reloadableFileManager != null) {
            return this.reloadableFileManager.inferBinaryName(location, file);
        }
        return result;
    }

    @Override
    public boolean handleOption(String current, Iterator<String> remaining) {
        boolean result = super.handleOption(current, remaining);
        if (!result && this.reloadableFileManager != null) {
            result = this.reloadableFileManager.handleOption(current, remaining);
        }
        return result;
    }

    @Override
    public boolean hasLocation(Location location) {
        return super.hasLocation(location) || (this.reloadableFileManager != null
                && this.reloadableFileManager.hasLocation(location));
    }

    @Override
    public JavaFileObject getJavaFileForInput(Location location, String className, JavaFileObject.Kind kind)
            throws IOException {
        JavaFileObject result = super.getJavaFileForInput(location, className, kind);
        if (result == null && this.reloadableFileManager != null) {
            result = this.reloadableFileManager.getJavaFileForInput(location, className, kind);
        }
        return result;
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind,
            FileObject sibling)
            throws IOException {
        JavaFileObject result = super.getJavaFileForOutput(location, className, kind, sibling);
        if (result == null && this.reloadableFileManager != null) {
            result = this.reloadableFileManager.getJavaFileForOutput(location, className, kind, sibling);
        }
        return result;
    }

    @Override
    public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
        FileObject result = super.getFileForInput(location, packageName, relativeName);
        if (result == null && this.reloadableFileManager != null) {
            result = this.reloadableFileManager.getFileForInput(location, packageName, relativeName);
        }
        return result;
    }

    @Override
    public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling)
            throws IOException {
        FileObject result = super.getFileForOutput(location, packageName, relativeName, sibling);
        if (result == null && this.reloadableFileManager != null) {
            result = this.reloadableFileManager.getFileForOutput(location, packageName, relativeName, sibling);
        }
        return result;
    }

    @Override
    public Location getLocationForModule(Location location, String moduleName) throws IOException {
        if (reloadableFileManager == null) {
            return super.getLocationForModule(location, moduleName);
        }
        final Location loc = super.getLocationForModule(location, moduleName);
        return loc != null ? loc : reloadableFileManager.getLocationForModule(location, moduleName);
    }

    @Override
    public Location getLocationForModule(Location location, JavaFileObject fo) throws IOException {
        if (reloadableFileManager == null) {
            return super.getLocationForModule(location, fo);
        }
        final Location loc = super.getLocationForModule(location, fo);
        return loc != null ? loc : reloadableFileManager.getLocationForModule(location, fo);
    }

    @Override
    public <S> ServiceLoader<S> getServiceLoader(Location location, Class<S> service) throws IOException {
        if (this.reloadableFileManager == null) {
            return super.getServiceLoader(location, service);
        }
        final ServiceLoader<S> result = super.getServiceLoader(location, service);
        return result != null ? result : this.reloadableFileManager.getServiceLoader(location, service);
    }

    @Override
    public String inferModuleName(Location location) throws IOException {
        if (this.reloadableFileManager == null) {
            return super.inferModuleName(location);
        }
        final String result = super.inferModuleName(location);
        return result != null ? result : this.reloadableFileManager.inferModuleName(location);
    }

    @Override
    public Iterable<Set<Location>> listLocationsForModules(Location location) {
        return combinedIterable(() -> {
            try {
                return super.listLocationsForModules(location);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, this.reloadableFileManager == null ? null : () -> {
            try {
                return this.reloadableFileManager.listLocationsForModules(location);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private <T> Iterable<T> combinedIterable(Supplier<Iterable<T>> required, Supplier<Iterable<T>> optional) {
        if (optional == null) {
            return required.get();
        }
        final Iterable<T> requiredIterable = required.get();
        if (requiredIterable == null) {
            return optional.get();
        }
        final Iterable<T> optionalIterable = optional.get();
        if (optionalIterable == null) {
            return requiredIterable;
        }
        return () -> {
            final Iterator<T> requiredI = requiredIterable.iterator();
            final Iterator<T> optionalI = optionalIterable.iterator();
            if (!requiredI.hasNext()) {
                return optionalI;
            }
            if (!optionalI.hasNext()) {
                return requiredI;
            }
            return new Iterator<>() {

                boolean currentIsStatic = true;

                @Override
                public boolean hasNext() {
                    return requiredI.hasNext() || optionalI.hasNext();
                }

                @Override
                public T next() {
                    currentIsStatic = requiredI.hasNext();
                    if (currentIsStatic) {
                        return requiredI.next();
                    }
                    return optionalI.next();
                }

                @Override
                public void remove() {
                    if (currentIsStatic) {
                        requiredI.remove();
                    } else {
                        optionalI.remove();
                    }
                }
            };
        };
    }

    @Override
    public boolean contains(Location location, FileObject fo) throws IOException {
        return super.contains(location, fo) || (this.reloadableFileManager != null
                && this.reloadableFileManager.contains(location, fo));
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaSources(Iterable<? extends File> files) {
        return this.fileManager.getJavaFileObjectsFromFiles(files);
    }

    @Override
    public void reset(Context context) {
        try {
            this.reloadableFileManager.close();
            this.reloadableFileManager.setLocation(StandardLocation.CLASS_PATH, context.getReloadableClassPath());
        } catch (IOException e) {
            throw new RuntimeException("Cannot reset reloadable file manager", e);
        }
        super.reset(context);
    }

    @Override
    public void close() throws IOException {
        this.reloadableFileManager.close();
        super.close();
    }

    /**
     * A class loader that combines multiple class loaders into one.<br>
     * The classes loaded by this class loader are associated with this class loader,
     * i.e. Class.getClassLoader() points to this class loader.
     */
    public static class JoinClassLoader extends ClassLoader {

        private final ClassLoader[] delegateClassLoaders;

        public JoinClassLoader(ClassLoader parent, ClassLoader... delegateClassLoaders) {
            super(parent);
            this.delegateClassLoaders = delegateClassLoaders;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            // It would be easier to call the loadClass() methods of the delegateClassLoaders
            // here, but we have to load the class from the bytecode ourselves, because we
            // need it to be associated with our class loader.
            String path = fromClassNameToResourceName(name);
            URL url = findResource(path);
            if (url == null) {
                throw new ClassNotFoundException(name);
            }
            ByteBuffer byteCode;
            try {
                byteCode = loadResource(url);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
            return defineClass(name, byteCode, null);
        }

        private ByteBuffer loadResource(URL url) throws IOException {
            return ClassPathUtils.readStream(url, stream -> {
                try {
                    return ByteBuffer.wrap(stream.readAllBytes());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }

        @Override
        protected URL findResource(String name) {
            for (ClassLoader delegate : delegateClassLoaders) {
                URL resource = delegate.getResource(name);
                if (resource != null) {
                    return resource;
                }
            }
            return null;
        }

        @Override
        protected Enumeration<URL> findResources(String name) throws IOException {
            Vector<URL> vector = new Vector<>();
            for (ClassLoader delegate : delegateClassLoaders) {
                Enumeration<URL> enumeration = delegate.getResources(name);
                while (enumeration.hasMoreElements()) {
                    vector.add(enumeration.nextElement());
                }
            }
            return vector.elements();
        }
    }
}
