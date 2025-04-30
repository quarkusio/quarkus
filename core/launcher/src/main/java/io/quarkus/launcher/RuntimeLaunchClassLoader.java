package io.quarkus.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

public class RuntimeLaunchClassLoader extends ClassLoader {

    static {
        registerAsParallelCapable();
    }

    public RuntimeLaunchClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String resourceName = "META-INF/ide-deps/" + name.replace(".", "/") + ".class.ide-launcher-res";
        try {
            try (InputStream is = getParent().getResourceAsStream(resourceName)) {
                if (is == null) {
                    throw new ClassNotFoundException(name);
                }
                definePackage(name);
                byte[] bytes = is.readAllBytes();

                return defineClass(name, bytes, 0, bytes.length);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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

    protected Class<?> findClass(String moduleName, String name) {
        try {
            return findClass(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    protected URL findResource(String moduleName, String name) throws IOException {
        return findResource(name);
    }

    @Override
    protected URL findResource(String name) {
        if (!name.startsWith("/")) {
            name = "/" + name;
        }
        return getParent().getResource("META-INF/ide-deps" + name + ".ide-launcher-res");
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        if (!name.startsWith("/")) {
            name = "/" + name;
        }
        return getParent().getResources("META-INF/ide-deps" + name + ".ide-launcher-res");
    }
}
