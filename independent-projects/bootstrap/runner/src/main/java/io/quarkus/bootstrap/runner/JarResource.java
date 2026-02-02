package io.quarkus.bootstrap.runner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import io.smallrye.common.io.jar.JarEntries;

/**
 * A jar resource
 */
public class JarResource implements ClassLoadingResource {

    private volatile ProtectionDomain protectionDomain;
    private final ManifestInfo manifestInfo;

    final Path jarPath;
    final AtomicReference<CompletableFuture<JarFileReference>> jarFileReference = new AtomicReference<>();

    public JarResource(ManifestInfo manifestInfo, Path jarPath) {
        this.manifestInfo = manifestInfo;
        this.jarPath = jarPath;
    }

    @Override
    public void init() {
        final URL url;
        try {
            String path = jarPath.toAbsolutePath().toString();
            if (!path.startsWith("/")) {
                path = '/' + path;
            }
            // we use this particular constructor to work around https://bugs.openjdk.org/browse/JDK-8140634
            // see https://github.com/quarkusio/quarkus/issues/52292
            URI uri = new URI("file", null, path, null, null);
            url = new URL((URL) null, uri.toString(), new JarUrlStreamHandler(uri));
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException("Unable to create protection domain for " + jarPath, e);
        }
        this.protectionDomain = new ProtectionDomain(new CodeSource(url, (Certificate[]) null), null);
    }

    @Override
    public byte[] getResourceData(String resource) {
        return JarFileReference.withJarFile(this, resource, JarResourceDataProvider.INSTANCE);
    }

    private static class JarResourceDataProvider implements JarFileReference.JarFileConsumer<byte[]> {
        private static final JarResourceDataProvider INSTANCE = new JarResourceDataProvider();

        @Override
        public byte[] apply(JarFile jarFile, Path path, String res) {
            ZipEntry entry = jarFile.getEntry(res);
            if (entry == null) {
                return null;
            }
            try (InputStream is = jarFile.getInputStream(entry)) {
                byte[] data = new byte[(int) entry.getSize()];
                int pos = 0;
                int rem = data.length;
                while (rem > 0) {
                    int read = is.read(data, pos, rem);
                    if (read == -1) {
                        throw new RuntimeException("Failed to read all data for " + res);
                    }
                    pos += read;
                    rem -= read;
                }
                return data;
            } catch (IOException e) {
                throw new RuntimeException("Failed to read zip entry " + res, e);
            }
        }
    }

    @Override
    public URL getResourceURL(String resource) {
        return JarFileReference.withJarFile(this, resource, JarResourceURLProvider.INSTANCE);
    }

    private static class JarResourceURLProvider implements JarFileReference.JarFileConsumer<URL> {
        private static final JarResourceURLProvider INSTANCE = new JarResourceURLProvider();

        @Override
        public URL apply(JarFile jarFile, Path path, String resource) {
            JarEntry entry = jarFile.getJarEntry(resource);
            if (entry == null) {
                return null;
            }
            try {
                final URL resUrl = getUrl(path, getRealName(entry, resource));
                // wrap it up into a "jar" protocol URL
                //horrible hack to deal with '?' characters in the URL
                //seems to be the only way, the URI constructor just does not let you handle them in a sane way
                var file = new StringBuilder((resUrl.getProtocol() == null ? 4 : resUrl.getProtocol().length()) + 1 +
                        resUrl.getPath().length() + (resUrl.getQuery() == null ? 0 : 3 + resUrl.getQuery().length()));
                // protocol shouldn't be null, but let's be safe
                file.append(resUrl.getProtocol());
                file.append(':');
                file.append(resUrl.getPath());
                if (resUrl.getQuery() != null) {
                    file.append("%3F");
                    file.append(resUrl.getQuery());
                }
                return new URL("jar", null, file.toString());
            } catch (MalformedURLException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        private static String getRealName(JarEntry entry, String resource) {
            String realName = JarEntries.getRealName(entry);
            // Make sure directories are returned with a / when the resource was requested with a /
            if (resource.endsWith("/") && entry.isDirectory()) {
                if (realName.endsWith("/")) {
                    return realName;
                } else {
                    return realName + "/";
                }
            }

            // this shouldn't be necessary but the previous implementation was doing it forcibly so keeping it for safety
            if (realName.endsWith("/")) {
                return realName.substring(0, realName.length() - 1);
            }

            return realName;
        }

        private static URL getUrl(Path jarPath, String realName) throws MalformedURLException, URISyntaxException {
            final URI jarUri = jarPath.toUri();
            // first create a URI which includes both the jar file path and the relative resource name
            // and then invoke a toURL on it. The URI reconstruction allows for any encoding to be done
            // for the "path" which includes the "realName"
            var ssp = new StringBuilder(jarUri.getPath().length() + realName.length() + 2);
            ssp.append(jarUri.getPath());
            ssp.append("!/");
            ssp.append(realName);
            // we use this particular constructor to work around https://bugs.openjdk.org/browse/JDK-8140634
            // see https://github.com/quarkusio/quarkus/issues/52292
            return new URI(jarUri.getScheme(), null, ssp.toString(), null, null).toURL();
        }
    }

    @Override
    public ManifestInfo getManifestInfo() {
        return manifestInfo;
    }

    @Override
    public ProtectionDomain getProtectionDomain() {
        return protectionDomain;
    }

    @Override
    public void close() {
        var futureRef = jarFileReference.get();
        if (futureRef != null) {
            // The jarfile has been already used and it's going to be removed from the cache,
            // so the future must be already completed
            var ref = futureRef.getNow(null);
            if (ref != null) {
                ref.markForClosing(this);
            }
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
        return jarPath.equals(that.jarPath);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(jarPath);
    }

    /**
     * This URLStreamHandler is designed to handle only one jar, which is the one passed in the constructor.
     * The goal here is to cache the external form of the URL.
     * <p>
     * Do not use this class outside of this extremely specific purpose.
     */
    private static class JarUrlStreamHandler extends URLStreamHandler {

        private final String externalForm;

        private JarUrlStreamHandler(URI uri) {
            this.externalForm = "file:".concat(uri.getRawPath());
            // while it would be more optimized to store the URI here for when we open connections
            // opening a connection for ProtectionDomains is actually extremely rare
            // and never done in production at runtime so we favored reducing memory allocations for the common case
        }

        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            return new JarURLConnection(u);
        }

        @Override
        protected String toExternalForm(URL u) {
            return externalForm;
        }
    }

    private static class JarURLConnection extends URLConnection {

        private final File file;

        private JarURLConnection(URL url) throws IOException {
            super(url);
            try {
                this.file = new File(url.toURI());
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        }

        @Override
        public void connect() throws IOException {
            if (!file.exists()) {
                throw new FileNotFoundException(file.getAbsolutePath());
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new FileInputStream(file);
        }

        @Override
        public int getContentLength() {
            return (int) file.length();
        }

        @Override
        public long getContentLengthLong() {
            return file.length();
        }

        @Override
        public String getContentType() {
            return "application/java-archive";
        }
    }
}
