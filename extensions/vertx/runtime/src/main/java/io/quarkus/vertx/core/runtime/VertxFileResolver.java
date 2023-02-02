package io.quarkus.vertx.core.runtime;

import static io.vertx.core.net.impl.URIDecoder.decodeURIComponent;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import io.netty.util.internal.PlatformDependent;
import io.vertx.core.VertxException;
import io.vertx.core.file.FileSystemOptions;
import io.vertx.core.spi.file.FileResolver;

/**
 * Sometimes the file resources of an application are bundled into jars, or are somewhere on the classpath but not
 * available on the file system, e.g. in the case of a Vert.x webapp bundled as a fat jar.
 * <p>
 * In this case we want the application to access the resource from the classpath as if it was on the file system.
 * <p>
 * We can do this by looking for the file on the classpath, and if found, copying it to a temporary cache directory
 * on disk and serving it from there.
 * <p>
 * There is one cache dir per Vert.x instance and they are deleted on Vert.x shutdown.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 * @author <a href="https://github.com/rworsnop/">Rob Worsnop</a>
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxFileResolver implements FileResolver {

    public static final String DISABLE_FILE_CACHING_PROP_NAME = "vertx.disableFileCaching";
    public static final String DISABLE_CP_RESOLVING_PROP_NAME = "vertx.disableFileCPResolving";
    public static final String CACHE_DIR_BASE_PROP_NAME = "vertx.cacheDirBase";
    private static final boolean NON_UNIX_FILE_SEP = File.separatorChar != '/';
    private static final String JAR_URL_SEP = "!/";

    private final File cwd;
    private final boolean enableCaching;
    private final boolean enableCPResolving;
    private final VertxFileCache cache;

    public VertxFileResolver() {
        this(new FileSystemOptions());
    }

    public VertxFileResolver(FileSystemOptions fileSystemOptions) {
        enableCaching = fileSystemOptions.isFileCachingEnabled();
        enableCPResolving = fileSystemOptions.isClassPathResolvingEnabled();

        if (enableCPResolving) {
            cache = VertxFileCache.setupCache(fileSystemOptions.getFileCacheDir());
        } else {
            cache = null;
        }

        String cwdOverride = System.getProperty("vertx.cwd");
        if (cwdOverride != null) {
            cwd = new File(cwdOverride).getAbsoluteFile();
        } else {
            cwd = null;
        }
    }

    public String cacheDir() {
        if (cache != null) {
            return cache.cacheDir();
        }
        return null;
    }

    VertxFileCache getFileCache() {
        return this.cache;
    }

    /**
     * Close this file resolver, this is a blocking operation.
     */
    public void close() throws IOException {
        if (enableCPResolving) {
            synchronized (cache) {
                cache.close();
            }
        }
    }

    public File resolveFile(String fileName) {
        // First look for file with that name on disk
        File file = new File(fileName);
        File resolved;
        boolean absolute = file.isAbsolute();

        if (cwd != null && !absolute) {
            resolved = new File(cwd, fileName);
        } else {
            resolved = file;
        }
        if (this.cache == null) {
            return resolved;
        }
        // We need to synchronized here to avoid 2 different threads to copy the file to the cache directory and so
        // corrupting the content.
        if (!resolved.exists()) {
            synchronized (cache) {
                // When an absolute file is here, if it falls under the cache directory, then it should be made relative as it
                // could mean that a previous resolution has been used to resolve a non local file system resource
                File cacheFile = cache.getCanonicalFile(file);
                // the file may or may not be in the cache dir after canonicalization
                if (cacheFile == null) {
                    // the given file cannot be resolved to the real FS
                    return file;
                }
                // compute the difference of the path
                String relativize = cache.relativize(cacheFile.getPath());
                if (relativize != null) {
                    // file is inside the cache dir, we can continue with the search
                    if (this.enableCaching && cacheFile.exists()) {
                        return cacheFile;
                    }
                    // as it wasn't found, maybe it hasn't been extracted yet
                    // adjust the name and absolute references if needed
                    if (absolute) {
                        fileName = relativize;
                        file = new File(relativize);
                        absolute = false;
                    }
                }
                // https://vertx.io/docs/vertx-core/java/#classpath
                // if a resource is still absolute, it means it's outside the cache, we don't need to handle it.
                // otherwise, we need to go over the classpath resources
                if (!absolute) {
                    // Look for file on classpath
                    ClassLoader cl = getClassLoader();

                    //https://github.com/eclipse/vert.x/issues/2126
                    //Cache all elements in the parent directory if it exists
                    //this is so that listing the directory after an individual file has
                    //been read works.
                    File parentFile = file.getParentFile();
                    while (parentFile != null) {
                        String parentFileName = parentFile.getPath();
                        if (NON_UNIX_FILE_SEP) {
                            parentFileName = parentFileName.replace(File.separatorChar, '/');
                        }
                        URL directoryContents = getValidClassLoaderResource(cl, parentFileName);
                        if (directoryContents != null) {
                            unpackUrlResource(directoryContents, parentFileName, cl, true);
                        }
                        // https://github.com/eclipse-vertx/vert.x/issues/3654
                        // go up one level until we reach the top, this way we ensure we extract the whole tree
                        // not just a branch so directory listing will be correct
                        parentFile = parentFile.getParentFile();
                    }

                    if (NON_UNIX_FILE_SEP) {
                        fileName = fileName.replace(File.separatorChar, '/');
                    }
                    URL url = getValidClassLoaderResource(cl, fileName);
                    if (url != null) {
                        return unpackUrlResource(url, fileName, cl, false);
                    }
                }
            }
        }
        return file;
    }

    private static boolean isValidWindowsCachePath(char c) {
        if (c < 32) {
            return false;
        }
        switch (c) {
            case '"':
            case '*':
            case ':':
            case '<':
            case '>':
            case '?':
            case '|':
                return false;
            default:
                return true;
        }
    }

    private static boolean isValidCachePath(String fileName) {
        if (PlatformDependent.isWindows()) {
            int len = fileName.length();
            for (int i = 0; i < len; i++) {
                char c = fileName.charAt(i);
                if (!isValidWindowsCachePath(c)) {
                    return false;
                }
                // Space only valid when it's not ending a name
                if (c == ' ' && (i + 1 == len || fileName.charAt(i + 1) == '/')) {
                    return false;
                }
            }
            return true;
        } else {
            return fileName.indexOf('\u0000') == -1;
        }
    }

    /**
     * Get a class loader resource that can unpack to a valid cache path.
     * <p>
     * Some valid entries are avoided purposely when we cannot create the corresponding file in the file cache.
     */
    private static URL getValidClassLoaderResource(ClassLoader cl, String fileName) {
        URL resource = cl.getResource(fileName);
        if (resource != null && !isValidCachePath(fileName)) {
            return null;
        }
        return resource;
    }

    private File unpackUrlResource(URL url, String fileName, ClassLoader cl, boolean isDir) {
        String prot = url.getProtocol();
        switch (prot) {
            case "file":
                return unpackFromFileURL(url, fileName, cl);
            case "jar":
                return unpackFromJarURL(url, fileName, cl);
            case "bundle": // Apache Felix, Knopflerfish
            case "bundleentry": // Equinox
            case "bundleresource": // Equinox
            case "jrt": // java run-time (JEP 220)
            case "resource": // substratevm (graal native image)
            case "vfs": // jboss-vfs
            case "quarkus": // QuarkusClassLoader
                return unpackFromBundleURL(url, fileName, isDir);
            default:
                throw new IllegalStateException("Invalid url protocol: " + prot);
        }
    }

    private File unpackFromFileURL(URL url, String fileName, ClassLoader cl) {
        final File resource = new File(decodeURIComponent(url.getPath(), false));
        boolean isDirectory = resource.isDirectory();
        File cacheFile;
        try {
            cacheFile = cache.cacheFile(fileName, resource, !enableCaching);
        } catch (IOException e) {
            throw new VertxException(getFileAccessErrorMessage("unpack", url.toString()), e);
        }
        if (isDirectory) {
            String[] listing = resource.list();
            if (listing != null) {
                for (String file : listing) {
                    String subResource = fileName + "/" + file;
                    URL url2 = getValidClassLoaderResource(cl, subResource);
                    if (url2 == null) {
                        throw new VertxException("Invalid resource: " + subResource);
                    }
                    unpackFromFileURL(url2, subResource, cl);
                }
            }
        }
        return cacheFile;
    }

    static String getFileAccessErrorMessage(String action, String path) {
        return "Unable to " + action + " file at path '" + path + "'";
    }

    private File unpackFromJarURL(URL url, String fileName, ClassLoader cl) {
        ZipFile zip = null;
        try {
            String path = url.getPath();
            int idx1 = path.lastIndexOf(".jar!");
            if (idx1 == -1) {
                idx1 = path.lastIndexOf(".zip!");
            }
            int idx2 = path.lastIndexOf(".jar!", idx1 - 1);
            if (idx2 == -1) {
                idx2 = path.lastIndexOf(".zip!", idx1 - 1);
            }
            if (idx2 == -1) {
                File file = new File(decodeURIComponent(path.substring(5, idx1 + 4), false));
                zip = new ZipFile(file);
            } else {
                String s = path.substring(idx2 + 6, idx1 + 4);
                File file = resolveFile(s);
                zip = new ZipFile(file);
            }

            String inJarPath = path.substring(idx1 + 6);
            StringBuilder prefixBuilder = new StringBuilder();
            int first = 0;
            int second;
            int len = JAR_URL_SEP.length();
            while ((second = inJarPath.indexOf(JAR_URL_SEP, first)) >= 0) {
                prefixBuilder.append(inJarPath, first, second).append("/");
                first = second + len;
            }
            String prefix = prefixBuilder.toString();
            Enumeration<? extends ZipEntry> entries = zip.entries();
            String prefixCheck = prefix.isEmpty() ? fileName : prefix + fileName;
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith(prefixCheck)) {
                    String p = prefix.isEmpty() ? name : name.substring(prefix.length());
                    if (name.endsWith("/")) {
                        // Directory
                        cache.cacheDir(p);
                    } else {
                        try (InputStream is = zip.getInputStream(entry)) {
                            cache.cacheFile(p, is, !enableCaching);
                        }
                    }

                }
            }
        } catch (IOException e) {
            throw new VertxException(getFileAccessErrorMessage("unpack", url.toString()), e);
        } finally {
            closeQuietly(zip);
        }

        return cache.getFile(fileName);
    }

    private void closeQuietly(Closeable zip) {
        if (zip != null) {
            try {
                zip.close();
            } catch (IOException e) {
                // Ignored.
            }
        }
    }

    /**
     * It is possible to determine if a resource from a bundle is a directory based on whether or not the ClassLoader
     * returns null for a path (which does not already contain a trailing '/') *and* that path with an added trailing '/'
     *
     * @param url the url
     * @return if the bundle resource represented by the bundle URL is a directory
     */
    private boolean isBundleUrlDirectory(URL url) {
        return url.toExternalForm().endsWith("/") ||
                getValidClassLoaderResource(getClassLoader(), url.getPath().substring(1) + "/") != null;
    }

    /**
     * bundle:// urls are used by OSGi implementations to refer to a file contained in a bundle, or in a fragment. There
     * is not much we can do to get the file from it, except reading it from the url. This method copies the files by
     * reading it from the url.
     *
     * @param url the url
     * @param fileName the file name used to cache the content
     * @return the extracted file
     */
    private File unpackFromBundleURL(URL url, String fileName, boolean isDir) {
        try {
            if ((getClassLoader() != null && isBundleUrlDirectory(url)) || isDir) {
                // Directory
                cache.cacheDir(fileName);
            } else {
                try (InputStream is = url.openStream()) {
                    cache.cacheFile(fileName, is, !enableCaching);
                }
            }
        } catch (IOException e) {
            throw new VertxException(getFileAccessErrorMessage("unpack", url.toString()), e);
        }
        return cache.getFile(fileName);
    }

    private ClassLoader getClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = getClass().getClassLoader();
        }
        // when running on substratevm (graal) the access to class loaders
        // is very limited and might be only available from compile time
        // known classes. (Object is always known, so we do a final attempt
        // to get it here).
        if (cl == null) {
            cl = Object.class.getClassLoader();
        }
        return cl;
    }
}
