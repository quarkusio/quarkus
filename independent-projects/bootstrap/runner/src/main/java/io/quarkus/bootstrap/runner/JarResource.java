package io.quarkus.bootstrap.runner;

import io.smallrye.common.io.jar.JarEntries;
import io.smallrye.common.io.jar.JarFiles;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
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
    private volatile JarFile zipFile;

    public JarResource(ManifestInfo manifestInfo, Path jarPath) {
        this.manifestInfo = manifestInfo;
        this.jarPath = jarPath;
    }

    @Override
    public byte[] getResourceData(String resource) {
        ZipFile zipFile = file();
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
    }

    @Override
    public URL getResourceURL(String resource) {
        JarFile zipFile = file();
        JarEntry entry = zipFile.getJarEntry(resource);
        if (entry == null) {
            return null;
        }
        try {
            String realName = JarEntries.getRealName(entry);
            // Avoid ending the URL with / to avoid breaking compatibility
            if (realName.endsWith("/")) {
                realName = realName.substring(0, realName.length() - 1);
            }
            URI jarUri = jarPath.toUri();
            return new URL("jar", null, jarUri.getScheme() + ":" + jarUri.getPath() + "!/" + resource);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ManifestInfo getManifestInfo() {
        return manifestInfo;
    }

    @Override
    public ProtectionDomain getProtectionDomain(ClassLoader classLoader) {
        URL url = null;
        try {
            String path = jarPath.toAbsolutePath().toString();
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            URI uri = new URI("file", null, path, null);
            url = uri.toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException("Unable to create protection domain for " + jarPath, e);
        }
        CodeSource codesource = new CodeSource(url, (Certificate[]) null);
        return new ProtectionDomain(codesource, null, classLoader, null);
    }

    private JarFile file() {
        if (zipFile == null) {
            synchronized (this) {
                if (zipFile == null) {
                    try {
                        return zipFile = JarFiles.create(jarPath.toFile());
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to open " + jarPath, e);
                    }
                }
            }
        }
        return zipFile;
    }

    @Override
    public void close() {
        if (zipFile != null) {
            try {
                zipFile.close();
            } catch (IOException e) {
                //ignore
            }
        }
    }
}
