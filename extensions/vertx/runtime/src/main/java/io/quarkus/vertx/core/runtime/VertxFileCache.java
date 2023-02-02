package io.quarkus.vertx.core.runtime;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.UUID;

import io.vertx.core.VertxException;
import io.vertx.core.file.impl.FileSystemImpl;
import io.vertx.core.impl.Utils;

class VertxFileCache {

    static VertxFileCache setupCache(String fileCacheDir) {
        VertxFileCache cache = new VertxFileCache(setupCacheDir(fileCacheDir));
        // Add shutdown hook to delete on exit
        cache.registerShutdownHook();
        return cache;
    }

    /**
     * Prepares the cache directory to be used in the application.
     */
    static File setupCacheDir(String fileCacheDir) {
        // ensure that the argument doesn't end with separator
        if (fileCacheDir.endsWith(File.separator)) {
            fileCacheDir = fileCacheDir.substring(0, fileCacheDir.length() - File.separator.length());
        }

        // the cacheDir will be suffixed a unique id to avoid eavesdropping from other processes/users
        // also this ensures that if process A deletes cacheDir, it won't affect process B
        String cacheDirName = fileCacheDir + "-" + UUID.randomUUID();
        File cacheDir = new File(cacheDirName);
        // Create the cache directory
        try {
            if (Utils.isWindows()) {
                Files.createDirectories(cacheDir.toPath());
            } else {
                // for security reasons, cache directory should not be readable/writable from other users
                // just like "POSIX mkdtemp(3)", the created directory should have 0700 permission
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwx------");
                Files.createDirectories(cacheDir.toPath(), PosixFilePermissions.asFileAttribute(perms));
            }
        } catch (IOException e) {
            throw new IllegalStateException(getFolderAccessErrorMessage("create", fileCacheDir), e);
        }
        return cacheDir;
    }

    static String getFolderAccessErrorMessage(String action, String path) {
        return "Unable to " + action + " folder at path '" + path + "'";
    }

    private Thread shutdownHook;
    private File cacheDir;

    public VertxFileCache(File cacheDir) {
        try {
            this.cacheDir = cacheDir.getCanonicalFile();
        } catch (IOException e) {
            throw new VertxException("Cannot get canonical name of cacheDir", e);
        }
    }

    synchronized void registerShutdownHook() {
        Thread shutdownHook = new Thread(this::runHook);
        this.shutdownHook = shutdownHook;
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private void runHook() {
        synchronized (this) {
            // no-op if cache dir has been set to null
            if (cacheDir == null) {
                return;
            }
        }
        Thread deleteCacheDirThread = new Thread(() -> {
            try {
                deleteCacheDir();
            } catch (IOException ignore) {
            }
        });
        // start the thread
        deleteCacheDirThread.start();
        try {
            deleteCacheDirThread.join(10 * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    String cacheDir() {
        return getCacheDir().getPath();
    }

    void close() throws IOException {
        final Thread hook;
        synchronized (this) {
            hook = shutdownHook;
            // disable the shutdown hook thread
            shutdownHook = null;
        }
        if (hook != null) {
            // May throw IllegalStateException if called from other shutdown hook so ignore that
            try {
                Runtime.getRuntime().removeShutdownHook(hook);
            } catch (IllegalStateException ignore) {
                // ignore
            }
        }
        deleteCacheDir();
    }

    private void deleteCacheDir() throws IOException {
        final File dir;
        synchronized (this) {
            if (cacheDir == null) {
                return;
            }
            // save the state before we force a flip
            dir = cacheDir;
            // disable the cache dir
            cacheDir = null;
        }
        // threads will only enter here once, as the resolving flag is flipped above
        if (dir.exists()) {
            FileSystemImpl.delete(dir.toPath(), true);
        }
    }

    File getFile(String fileName) {
        return new File(getCacheDir(), fileName);
    }

    File getCanonicalFile(File file) {
        try {
            if (file.isAbsolute()) {
                return file.getCanonicalFile();
            } else {
                return getFile(file.getPath()).getCanonicalFile();
            }
        } catch (IOException e) {
            // if we can't get the canonical form, return null
            return null;
        }
    }

    String relativize(String fileName) {
        String cachePath = getCacheDir().getPath();

        if (fileName.startsWith(cachePath)) {
            int cachePathLen = cachePath.length();
            if (fileName.length() == cachePathLen) {
                return "";
            } else {
                if (fileName.charAt(cachePathLen) == File.separatorChar) {
                    return fileName.substring(cachePathLen + 1);
                }
            }
        }
        return null;
    }

    File cacheFile(String fileName, File resource, boolean overwrite) throws IOException {
        File cacheFile = new File(getCacheDir(), fileName);
        fileNameCheck(cacheFile);
        boolean isDirectory = resource.isDirectory();
        if (!isDirectory) {
            cacheFile.getParentFile().mkdirs();
            if (!overwrite) {
                try {
                    Files.copy(resource.toPath(), cacheFile.toPath());
                } catch (FileAlreadyExistsException ignore) {
                }
            } else {
                Files.copy(resource.toPath(), cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } else {
            cacheFile.mkdirs();
        }
        return cacheFile;
    }

    void cacheFile(String fileName, InputStream is, boolean overwrite) throws IOException {
        File cacheFile = new File(getCacheDir(), fileName);
        fileNameCheck(cacheFile);
        cacheFile.getParentFile().mkdirs();
        if (!overwrite) {
            try {
                Files.copy(is, cacheFile.toPath());
            } catch (FileAlreadyExistsException ignore) {
            }
        } else {
            Files.copy(is, cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    void cacheDir(String fileName) throws IOException {
        File file = new File(getCacheDir(), fileName);
        fileNameCheck(file);
        file.mkdirs();
    }

    private void fileNameCheck(File file) throws IOException {
        if (!file.getCanonicalFile().toPath().startsWith(getCacheDir().toPath())) {
            throw new VertxException("File is outside of the cacheDir dir: " + file);
        }
    }

    private File getCacheDir() {
        File currentCacheDir = cacheDir;
        if (currentCacheDir == null) {
            throw new IllegalStateException("cacheDir has been removed. FileResolver is closing?");
        }
        return currentCacheDir;
    }
}
