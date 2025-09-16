package io.quarkus.bootstrap.runner;

import static io.quarkus.commons.classloading.ClassLoaderHelper.fromClassNameToResourceName;
import static io.quarkus.commons.classloading.ClassLoaderHelper.isInJdkPackage;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.crac.Context;
import org.crac.Resource;

/**
 * Classloader used with the fast-jar package type.
 *
 * This ClassLoader takes advantage of the fact that Quarkus knows the entire classpath when the application is built,
 * and thus can index the location of classes and resources (with the result being written during the build to a binary file and
 * read at application startup).
 * The advantage this has over the JDK's default System ClassLoader is that it knows which jars contain the requested
 * classes and resources and thus does not have to iterate over all the jars on the classpath
 * (which is slow and takes up heap space as the jars need to be kept open), but instead can go directly to the
 * jar(s) containing the requested class or resource.
 * The implementation also contains optimizations that allow the ClassLoader to keep a minimum number of jars open
 * while also preventing the lookup of the entire classpath for missing resources in known directories (like META-INF/services).
 */
public final class RunnerClassLoader extends ClassLoader {

    static {
        registerAsParallelCapable();
    }

    /**
     * A map of resources by dir name. Root dir/default package is represented by the empty string
     */
    private final Map<String, ClassLoadingResource[]> resourceDirectoryMap;

    private final Set<String> parentFirstPackages;
    private final Set<String> nonExistentResources;
    // the following two fields go hand in hand - they need to both be populated from the same data
    // in order for the resource loading to work properly
    // normally this field would be a set, but it only contains 2 elements, so making it a list is actually better
    private final List<String> fullyIndexedDirectories;
    private final Map<String, ClassLoadingResource[]> directlyIndexedResourcesIndexMap;

    //Mutations protected by synchronization on the field value itself:
    private final ClassLoadingResource[] currentlyBufferedResources = new ClassLoadingResource[4];//Experimentally found to be a reasonable number
    //Protected by synchronization on the above field, as they are related.
    private boolean postBootPhase = false;

    private final CracResource resource;

    RunnerClassLoader(ClassLoader parent, Map<String, ClassLoadingResource[]> resourceDirectoryMap,
            Set<String> parentFirstPackages, Set<String> nonExistentResources,
            List<String> fullyIndexedDirectories, Map<String, ClassLoadingResource[]> directlyIndexedResourcesIndexMap) {
        super(parent);
        this.resourceDirectoryMap = resourceDirectoryMap;
        this.parentFirstPackages = parentFirstPackages;
        this.nonExistentResources = nonExistentResources;
        this.fullyIndexedDirectories = fullyIndexedDirectories;
        this.directlyIndexedResourcesIndexMap = directlyIndexedResourcesIndexMap;

        resource = new CracResource();
        org.crac.Core.getGlobalContext().register(resource);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        //note that for performance reasons this CL does not do parent first delegation
        //although the intention is not for it to be a true isolated parent first CL
        //'delegation misses' where the parent throws a ClassNotFoundException are very expensive
        if (isInJdkPackage(name)) {
            return getParent().loadClass(name);
        }
        String packageName = getPackageNameFromClassName(name);
        if (parentFirstPackages.contains(packageName)) {
            try {
                return getParent().loadClass(name);
            } catch (ClassNotFoundException e) {
                //fall through
            }
        }
        Class<?> loaded = findLoadedClass(name);
        if (loaded != null) {
            return loaded;
        }
        final ClassLoadingResource[] resources;
        if (packageName == null) {
            resources = resourceDirectoryMap.get("");
        } else {
            String dirName = packageName.replace('.', '/');
            resources = resourceDirectoryMap.get(dirName);
        }
        if (resources != null) {
            String classResource = fromClassNameToResourceName(name);
            for (ClassLoadingResource resource : resources) {
                accessingResource(resource);
                byte[] data = resource.getResourceData(classResource);
                if (data == null) {
                    continue;
                }
                definePackage(packageName, resources);
                return defineClass(name, data, resource);
            }
        }
        return getParent().loadClass(name);
    }

    private void definePackage(String pkgName, ClassLoadingResource[] resources) {
        if ((pkgName != null) && getDefinedPackage(pkgName) == null) {
            for (ClassLoadingResource classPathElement : resources) {
                ManifestInfo mf = classPathElement.getManifestInfo();
                if (mf != null) {
                    try {
                        definePackage(pkgName, mf.getSpecTitle(),
                                mf.getSpecVersion(),
                                mf.getSpecVendor(),
                                mf.getImplTitle(),
                                mf.getImplVersion(),
                                mf.getImplVendor(), null);
                    } catch (IllegalArgumentException e) {
                        var loaded = getDefinedPackage(pkgName);
                        if (loaded == null) {
                            throw e;
                        }
                    }
                    return;
                }
            }
            try {
                definePackage(pkgName, null, null, null, null, null, null, null);
            } catch (IllegalArgumentException e) {
                var loaded = getDefinedPackage(pkgName);
                if (loaded == null) {
                    throw e;
                }
            }
        }
    }

    private static int u8(byte[] data, int offset) {
        return Byte.toUnsignedInt(data[offset]);
    }

    private static int u16(byte[] data, int offset) {
        return u8(data, offset) << 8 | u8(data, offset + 1);
    }

    private static String binaryName(byte[] data, int offset, int length) {
        // maybe larger than needed
        char[] ca = new char[length];
        int j = 0;
        for (int i = 0; i < length;) {
            int lb = u8(data, offset + i);
            if (lb < 0x80) {
                if (lb == '/') {
                    lb = '.';
                }
                ca[j++] = (char) lb;
                i++;
            } else if (lb < 0xC0) {
                throw new ClassFormatError("Illegal character in class name: " + lb);
            } else if (lb < 0xE0) {
                ca[j++] = (char) ((lb & 0x1F) << 6 | u8(data, offset + i + 1) & 0x3F);
                i += 2;
            } else if (lb < 0xF0) {
                ca[j++] = (char) ((lb & 0x0F) << 12 | (u8(data, offset + i + 2) & 0x3F) << 6 | u8(data, offset + i + 2) & 0x3F);
                i += 3;
            } else {
                throw new ClassFormatError("Illegal character in class name: " + lb);
            }
        }
        return new String(ca, 0, j);
    }

    private static final int O_CP_COUNT = 8;
    private static final int O_CP_START = 10;
    private static final int O_SUPER_FROM_CP_END = 4;

    private static final int CONSTANT_Utf8 = 1;
    private static final int CONSTANT_Integer = 3;
    private static final int CONSTANT_Float = 4;
    private static final int CONSTANT_Long = 5;
    private static final int CONSTANT_Double = 6;
    private static final int CONSTANT_Class = 7;
    private static final int CONSTANT_String = 8;
    private static final int CONSTANT_Fieldref = 9;
    private static final int CONSTANT_Methodref = 10;
    private static final int CONSTANT_InterfaceMethodref = 11;
    private static final int CONSTANT_NameAndType = 12;
    private static final int CONSTANT_MethodHandle = 15;
    private static final int CONSTANT_MethodType = 16;
    private static final int CONSTANT_Dynamic = 17;
    private static final int CONSTANT_InvokeDynamic = 18;
    private static final int CONSTANT_Module = 19;
    private static final int CONSTANT_Package = 20;

    private static final byte[] javaLangObject = "java/lang/Object".getBytes(StandardCharsets.US_ASCII);

    private Class<?> defineClass(String name, byte[] data, ClassLoadingResource resource) {
        Class<?> loaded = findLoadedClass(name);
        if (loaded != null) {
            return loaded;
        }
        // scan class bytes and preload supertype(s)
        try {
            int cpCnt = u16(data, O_CP_COUNT);
            int offs = O_CP_START;
            int[] cpOffs = new int[cpCnt];
            for (int i = 1; i < cpCnt; i++) {
                int tag = u8(data, offs);
                cpOffs[i] = offs;
                offs += switch (tag) {
                    case CONSTANT_Class, CONSTANT_MethodType, CONSTANT_Module, CONSTANT_String, CONSTANT_Package -> 3;
                    case CONSTANT_MethodHandle -> 4;
                    case CONSTANT_Integer, CONSTANT_Float, CONSTANT_Fieldref, CONSTANT_Methodref, CONSTANT_InterfaceMethodref,
                            CONSTANT_NameAndType, CONSTANT_Dynamic, CONSTANT_InvokeDynamic ->
                        5;
                    case CONSTANT_Long, CONSTANT_Double -> {
                        i++;
                        yield 9;
                    }
                    case CONSTANT_Utf8 -> u16(data, offs + 1) + 3;
                    default -> throw new ClassFormatError(
                            "Invalid tag: " + tag + " at offset " + offs + " (entry " + i + ") for class " + name);
                };
            }
            int superCp = u16(data, offs + O_SUPER_FROM_CP_END);
            if (superCp != 0 && u8(data, cpOffs[superCp]) == CONSTANT_Class) {
                int superCi = u16(data, cpOffs[superCp] + 1);
                int superStrOffs = cpOffs[superCi];
                if (superCi != 0 && u8(data, superStrOffs) == CONSTANT_Utf8) {
                    int superLen = u16(data, superStrOffs + 1);
                    // only load the class if it isn't java.lang.Object
                    int superStrStart = superStrOffs + 3;
                    if (!Arrays.equals(data, superStrStart, superStrStart + superLen, javaLangObject, 0,
                            javaLangObject.length)) {
                        String superName = binaryName(data, superStrStart, superLen);
                        try {
                            loadClass(superName);
                        } catch (ClassNotFoundException e) {
                            throw new LinkageError("Failed to load super class " + superName + " of class " + name, e);
                        }
                        loaded = findLoadedClass(name);
                        if (loaded != null) {
                            return loaded;
                        }
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            ClassFormatError error = new ClassFormatError("Failed to load class " + name);
            error.initCause(e);
            throw error;
        }
        try {
            return defineClass(name, data, 0, data.length, resource.getProtectionDomain());
        } catch (LinkageError e) {
            loaded = findLoadedClass(name);
            if (loaded != null) {
                return loaded;
            }
            throw e;
        }
    }

    private void accessingResource(final ClassLoadingResource resource) {
        final ClassLoadingResource toEvict;
        synchronized (this.currentlyBufferedResources) {
            if (!postBootPhase) {
                //We only want to limit the jar buffers after the initial bootstrap has been completed
                return;
            }
            // This is not a cache aiming to accurately retain the most hot resources:
            // it's too small to benefit from traditional hit metrics,
            // we rather prefer to keep it very light.
            if (currentlyBufferedResources[0] == resource) {
                //it's already on the head of the cache: nothing to be done.
                return;
            }

            for (int i = 1; i < currentlyBufferedResources.length; i++) {
                final ClassLoadingResource currentI = currentlyBufferedResources[i];
                if (currentI == resource || currentI == null) {
                    //it was already cached, or we found an empty slot: bubble it up by one position to give it a boost
                    bubbleUpCachedResource(resource, i);
                    return;
                }
            }

            // else, we drop one element from the cache,
            // and inserting the latest resource on the tail:
            toEvict = currentlyBufferedResources[currentlyBufferedResources.length - 1];
            bubbleUpCachedResource(resource, currentlyBufferedResources.length - 1);
        }

        // Finally, release the cache for the dropped element:
        toEvict.resetInternalCaches();
    }

    private void bubbleUpCachedResource(ClassLoadingResource resource, int i) {
        for (int j = i; j > 0; j--) {
            currentlyBufferedResources[j] = currentlyBufferedResources[j - 1];
        }
        currentlyBufferedResources[0] = resource;
    }

    @Override
    protected URL findResource(String name) {
        name = sanitizeName(name);
        if (nonExistentResources.contains(name)) {
            return null;
        }
        ClassLoadingResource[] resources = getClassLoadingResources(name);
        if (resources == null)
            return null;
        for (ClassLoadingResource resource : resources) {
            accessingResource(resource);
            URL data = resource.getResourceURL(name);
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    private String sanitizeName(final String name) {
        if (name.length() > 0 && name.charAt(0) == '/') {
            return name.substring(1);
        }
        return name;
    }

    private ClassLoadingResource[] getClassLoadingResources(final String name) {
        ClassLoadingResource[] resources = directlyIndexedResourcesIndexMap.get(name);
        if (resources != null) {
            return resources;
        }
        String dirName = getDirNameFromResourceName(name);
        if (dirName == null) {
            dirName = "";
        }
        if (!dirName.equals(name) && fullyIndexedDirectories.contains(dirName)) {
            if (dirName.isEmpty()) {
                return resourceDirectoryMap.get(name);
            }
            // If we arrive here, we know that resource being queried belongs to one of the fully indexed directories
            // Had that resource existed however, it would have been present in directlyIndexedResourcesIndexMap
            return null;
        }
        resources = resourceDirectoryMap.get(dirName);
        if (resources == null) {
            // the resource could itself be a directory
            resources = resourceDirectoryMap.get(name);
        }
        return resources;
    }

    @Override
    protected Enumeration<URL> findResources(String name) {
        name = sanitizeName(name);
        if (nonExistentResources.contains(name)) {
            return Collections.emptyEnumeration();
        }
        ClassLoadingResource[] resources = getClassLoadingResources(name);
        if (resources == null)
            return Collections.emptyEnumeration();
        List<URL> urls = new ArrayList<>();
        for (ClassLoadingResource resource : resources) {
            accessingResource(resource);
            URL data = resource.getResourceURL(name);
            if (data != null) {
                urls.add(data);
            }
        }
        return Collections.enumeration(urls);
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

    public void resetInternalCaches() {
        synchronized (this.currentlyBufferedResources) {
            for (Map.Entry<String, ClassLoadingResource[]> entry : resourceDirectoryMap.entrySet()) {
                for (ClassLoadingResource i : entry.getValue()) {
                    i.resetInternalCaches();
                }
            }
            this.postBootPhase = true;
        }
    }

    class CracResource implements Resource {
        @Override
        public void beforeCheckpoint(Context<? extends Resource> ctx) {
            synchronized (currentlyBufferedResources) {
                for (int i = 0; i < currentlyBufferedResources.length; ++i) {
                    if (currentlyBufferedResources[i] != null) {
                        currentlyBufferedResources[i].resetInternalCaches();
                        currentlyBufferedResources[i] = null;
                    }
                }
            }
        }

        @Override
        public void afterRestore(Context<? extends Resource> ctx) {
        }
    }

    @Override
    public boolean equals(Object o) {
        //see comment in hashCode
        return this == o;
    }

    @Override
    public int hashCode() {
        //We can return a constant as we expect to have a single instance of these;
        //this is useful to avoid triggering a call to the identity hashcode,
        //which could be rather inefficient as there's good chances that some component
        //will have inflated the monitor of this instance.
        //A hash collision would be unfortunate but unexpected, and shouldn't be a problem
        //as the equals implementation still does honour the identity contract .
        //See also discussion on https://github.com/smallrye/smallrye-context-propagation/pull/443
        return 1;
    }

}
