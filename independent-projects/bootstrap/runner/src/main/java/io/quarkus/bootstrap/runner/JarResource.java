package io.quarkus.bootstrap.runner;

import java.io.ByteArrayOutputStream;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A jar resource
 */
public class JarResource implements ClassLoadingResource {

    private final ManifestInfo manifestInfo;
    private final Path jarPath;
    private volatile ZipFile zipFile;

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
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] data = new byte[1024];
            int r;
            while ((r = is.read(data)) > 0) {
                out.write(data, 0, r);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read zip entry " + resource, e);
        }
    }

    @Override
    public URL getResourceURL(String resource) {
        ZipFile zipFile = file();
        ZipEntry entry = zipFile.getEntry(resource);
        if (entry == null) {
            return null;
        }
        try {
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
            URI uri = new URI("jar:file", null, jarPath.toAbsolutePath().toString() + "!/", null);
            url = uri.toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException("Unable to create protection domain for " + jarPath, e);
        }
        CodeSource codesource = new CodeSource(url, (Certificate[]) null);
        ProtectionDomain protectionDomain = new ProtectionDomain(codesource, null, classLoader, null);
        return protectionDomain;
    }

    private ZipFile file() {
        if (zipFile == null) {
            synchronized (this) {
                if (zipFile == null) {
                    try {
                        return zipFile = new ZipFile(jarPath.toFile());
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to open " + jarPath, e);
                    }
                }
            }
        }
        return zipFile;
    }
}
