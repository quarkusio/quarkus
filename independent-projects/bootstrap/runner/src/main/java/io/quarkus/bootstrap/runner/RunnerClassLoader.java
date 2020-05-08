package io.quarkus.bootstrap.runner;

import java.io.IOException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * Classloader used for production application, using the multi jar strategy
 *
 */
public class RunnerClassLoader extends ClassLoader {

    /**
     * A map of resources by dir name. Root dir/default package is represented by the empty string
     */
    private final Map<String, ClassLoadingResource[]> resourceDirectoryMap;

    private final ConcurrentMap<ClassLoadingResource, ProtectionDomain> protectionDomains = new ConcurrentHashMap<>();

    static {
        registerAsParallelCapable();
    }

    RunnerClassLoader(ClassLoader parent, Map<String, ClassLoadingResource[]> resourceDirectoryMap) {
        super(parent);
        this.resourceDirectoryMap = resourceDirectoryMap;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String packageName = getPackageNameFromClassName(name);
        ClassLoadingResource[] resources;
        if (packageName == null) {
            resources = resourceDirectoryMap.get("");
        } else {
            String dirName = packageName.replace(".", "/");
            resources = resourceDirectoryMap.get(dirName);
        }
        if (resources == null) {
            throw new ClassNotFoundException(name);
        }
        String classResource = name.replace(".", "/") + ".class";
        for (ClassLoadingResource resource : resources) {
            byte[] data = resource.getResourceData(classResource);
            if (data == null) {
                continue;
            }
            definePackage(packageName, resources);
            return defineClass(name, data, 0, data.length,
                    protectionDomains.computeIfAbsent(resource, new Function<ClassLoadingResource, ProtectionDomain>() {
                        @Override
                        public ProtectionDomain apply(ClassLoadingResource ce) {
                            return ce.getProtectionDomain(RunnerClassLoader.this);
                        }
                    }));
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    protected URL findResource(String name) {
        name = sanitizeName(name);
        ClassLoadingResource[] resources = getClassLoadingResources(name);
        if (resources == null)
            return null;
        for (ClassLoadingResource resource : resources) {
            URL data = resource.getResourceURL(name);
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    private String sanitizeName(String name) {
        if (name.startsWith("/")) {
            return name.substring(1);
        }
        return name;
    }

    private ClassLoadingResource[] getClassLoadingResources(String name) {
        String dirName = getDirNameFromResourceName(name);
        ClassLoadingResource[] resources;
        if (dirName == null) {
            resources = resourceDirectoryMap.get("");
        } else {
            resources = resourceDirectoryMap.get(dirName);
        }
        if (resources == null) {
            // the resource could itself be a directory
            resources = resourceDirectoryMap.get(name);
        }
        return resources;
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        name = sanitizeName(name);
        ClassLoadingResource[] resources = getClassLoadingResources(name);
        if (resources == null)
            return null;
        List<URL> urls = new ArrayList<>();
        for (ClassLoadingResource resource : resources) {
            URL data = resource.getResourceURL(name);
            if (data != null) {
                urls.add(data);
            }
        }
        return Collections.enumeration(urls);
    }

    private void definePackage(String pkgName, ClassLoadingResource[] resources) {
        if ((pkgName != null) && getPackage(pkgName) == null) {
            synchronized (getClassLoadingLock(pkgName)) {
                if (getPackage(pkgName) == null) {
                    for (ClassLoadingResource classPathElement : resources) {
                        ManifestInfo mf = classPathElement.getManifestInfo();
                        if (mf != null) {
                            definePackage(pkgName, mf.getSpecTitle(),
                                    mf.getSpecVersion(),
                                    mf.getSpecVendor(),
                                    mf.getImplTitle(),
                                    mf.getImplVersion(),
                                    mf.getImplVendor(), null);
                            return;
                        }
                    }
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

    private String getDirNameFromResourceName(String resourceName) {
        final int index = resourceName.lastIndexOf('/');
        if (index == -1) {
            // we return null here since in this case no package is defined
            // this is same behavior as Package.getPackage(clazz) exhibits
            // when the class is in the default package
            return null;
        }
        return resourceName.substring(0, index);
    }

    /**
     * This method is needed to make packages work correctly on JDK9+, as it will be called
     * to load the package-info class.
     *
     * @param moduleName
     * @param name
     * @return
     */
    //@Override
    protected Class<?> findClass(String moduleName, String name) {
        try {
            return loadClass(name, false);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public void close() {
        for (Map.Entry<String, ClassLoadingResource[]> entry : resourceDirectoryMap.entrySet()) {
            for (ClassLoadingResource i : entry.getValue()) {
                i.close();
            }
        }
    }
}
