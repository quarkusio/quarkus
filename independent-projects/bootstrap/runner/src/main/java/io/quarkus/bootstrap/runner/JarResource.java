package io.quarkus.bootstrap.runner;

import io.smallrye.common.io.jar.JarEntries;
import io.smallrye.common.io.jar.JarFiles;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A jar resource
 */
public class JarResource implements ClassLoadingResource {

    private final ManifestInfo manifestInfo;
    private final Path jarPath;

    private final Lock readLock;
    private final Lock writeLock;

    private volatile ProtectionDomain protectionDomain;

    //Guarded by the read/write lock; open/close operations on the JarFile require the exclusive lock,
    //while using an existing open reference can use the shared lock.
    //If a lock is acquired, and as long as it's owned, we ensure that the zipFile reference
    //points to an open JarFile instance, and read operations are valid.
    //To close the jar, the exclusive lock must be owned, and reference will be set to null before releasing it.
    //Likewise, opening a JarFile requires the exclusive lock.
    private volatile JarFile zipFile;

    // use a VarHandle to access the protectionDomain as the value is written only by the main thread
    // and all other threads simply read the value, and thus we can use the Release / Acquire access mode
    private static final VarHandle PROTECTION_DOMAIN_VH;

    static {
        try {
            PROTECTION_DOMAIN_VH = MethodHandles.lookup().findVarHandle(JarResource.class, "protectionDomain",
                    ProtectionDomain.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

    public JarResource(ManifestInfo manifestInfo, Path jarPath) {
        this.manifestInfo = manifestInfo;
        this.jarPath = jarPath;
        final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        this.readLock = readWriteLock.readLock();
        this.writeLock = readWriteLock.writeLock();
    }

    @Override
    public void init(ClassLoader runnerClassLoader) {
        final URL url;
        try {
            String path = jarPath.toAbsolutePath().toString();
            if (!path.startsWith("/")) {
                path = '/' + path;
            }
            URI uri = new URI("file", null, path, null);
            url = uri.toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException("Unable to create protection domain for " + jarPath, e);
        }
        PROTECTION_DOMAIN_VH.setRelease(this,
                new ProtectionDomain(new CodeSource(url, (Certificate[]) null), null, runnerClassLoader, null));
    }

    @Override
    public byte[] getResourceData(String resource) {
        final ZipFile zipFile = readLockAcquireAndGetJarReference();
        try {
            ZipEntry entry = zipFile.getEntry(resource);
            if (entry == null) {
                return null;
            }
            try (InputStream is = zipFile.getInputStream(entry)) {
                byte[] data = new byte[(int) entry.getSize()];
                int pos = 0;
                int rem = data.length;
                while (rem > 0) {
                    int read = is.read(data, pos, rem);
                    if (read == -1) {
                        throw new RuntimeException("Failed to read all data for " + resource);
                    }
                    pos += read;
                    rem -= read;
                }
                return data;
            } catch (IOException e) {
                throw new RuntimeException("Failed to read zip entry " + resource, e);
            }
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public URL getResourceURL(String resource) {
        final JarFile jarFile = readLockAcquireAndGetJarReference();
        try {
            JarEntry entry = jarFile.getJarEntry(resource);
            if (entry == null) {
                return null;
            }
            try {
                String realName = JarEntries.getRealName(entry);
                // Avoid ending the URL with / to avoid breaking compatibility
                if (realName.endsWith("/")) {
                    realName = realName.substring(0, realName.length() - 1);
                }
                final URI jarUri = jarPath.toUri();
                // first create a URI which includes both the jar file path and the relative resource name
                // and then invoke a toURL on it. The URI reconstruction allows for any encoding to be done
                // for the "path" which includes the "realName"
                final URL resUrl = new URI(jarUri.getScheme(), jarUri.getPath() + "!/" + realName, null).toURL();
                // wrap it up into a "jar" protocol URL
                return new URL("jar", null, resUrl.getProtocol() + ':' + resUrl.getPath());
            } catch (MalformedURLException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public ManifestInfo getManifestInfo() {
        return manifestInfo;
    }

    @Override
    public ProtectionDomain getProtectionDomain() {
        return (ProtectionDomain) PROTECTION_DOMAIN_VH.getAcquire(this);
    }

    private JarFile readLockAcquireAndGetJarReference() {
        while (true) {
            readLock.lock();
            final JarFile zipFileLocal = this.zipFile;
            if (zipFileLocal != null) {
                //Expected fast path: returns a reference to the open JarFile while owning the readLock
                return zipFileLocal;
            } else {
                //This Lock implementation doesn't allow upgrading a readLock to a writeLock, so release it
                //as we're going to need the WriteLock.
                readLock.unlock();
                //trigger the JarFile being (re)opened.
                ensureJarFileIsOpen();
                //Now since we no longer own any lock, we need to try again to obtain the readLock
                //and check for the reference still being valid.
                //This exposes us to a race with closing the just-opened JarFile;
                //however this should be extremely rare, so we can trust we won't loop much;
                //A counter doesn't seem necessary, as in fact we know that methods close()
                //and resetInternalCaches() are invoked each at most once, which limits the amount
                //of loops here in practice.
            }
        }
    }

    private void ensureJarFileIsOpen() {
        writeLock.lock();
        try {
            if (this.zipFile == null) {
                try {
                    this.zipFile = JarFiles.create(jarPath.toFile());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to open " + jarPath, e);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void close() {
        writeLock.lock();
        try {
            final JarFile zipFileLocal = this.zipFile;
            if (zipFileLocal != null) {
                try {
                    zipFileLocal.close();
                } catch (IOException e) {
                    //ignore
                }
                this.zipFile = null;
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void resetInternalCaches() {
        //Currently same implementations as #close
        close();
    }

    @Override
    public String toString() {
        return "JarResource{" +
                jarPath.getFileName() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        JarResource that = (JarResource) o;
        return Objects.equals(manifestInfo, that.manifestInfo) && jarPath.equals(that.jarPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(manifestInfo, jarPath);
    }
}
