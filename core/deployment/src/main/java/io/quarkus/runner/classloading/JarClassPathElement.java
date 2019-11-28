package io.quarkus.runner.classloading;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import io.quarkus.deployment.util.FileUtil;

/**
 * A class path element that represents a file on the file system
 */
public class JarClassPathElement implements ClassPathElement {

    private final URL jarPath;
    private final JarFile jarFile;

    public JarClassPathElement(Path root) {
        try {
            jarPath = root.toUri().toURL();
            jarFile = new JarFile(root.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ClassPathResource getResource(String name) {
        ZipEntry res = jarFile.getEntry(name);
        if (res != null) {
            return new ClassPathResource() {
                @Override
                public ClassPathElement getContainingElement() {
                    return JarClassPathElement.this;
                }

                @Override
                public String getPath() {
                    return name;
                }

                @Override
                public URL getUrl() {
                    try {
                        return new URL("jar:" + jarPath.getProtocol(), null, jarPath.getPath() + "!/" + name);
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public byte[] getData() {
                    try {
                        return FileUtil.readFileContents(jarFile.getInputStream(res));
                    } catch (IOException e) {
                        throw new RuntimeException("Unable to read " + name, e);
                    }
                }
            };
        }
        return null;
    }

    @Override
    public Set<String> getProvidedResources() {
        Set<String> paths = new HashSet<>();
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            paths.add(entry.getName());
        }
        return paths;
    }

    @Override
    public ProtectionDomain getProtectionDomain(ClassLoader classLoader) {
        URL url = null;
        try {
            URI uri = new URI("jar:file", null, jarPath.getPath() + "!/", null);
            url = uri.toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException("Unable to create protection domain for " + jarPath, e);
        }
        CodeSource codesource = new CodeSource(url, (Certificate[]) null);
        ProtectionDomain protectionDomain = new ProtectionDomain(codesource, null, classLoader, null);
        return protectionDomain;
    }

    @Override
    public void close() throws IOException {
        jarFile.close();
    }
}
