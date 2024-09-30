package io.quarkus.vertx.http.runtime.devmode;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import io.quarkus.fs.util.ZipUtils;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.impl.HttpUtils;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.core.net.impl.URIDecoder;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.Utils;

/**
 * A Handler to serve static files from jar files or from a local directory.
 */
public class FileSystemStaticHandler implements Handler<RoutingContext>, Closeable {

    private static final String DEFAULT_CONTENT_ENCODING = StandardCharsets.UTF_8.name();

    private static final ReentrantLock ROOT_CREATION_LOCK = new ReentrantLock();

    /**
     * Resolved web roots based on the configured web roots.
     */
    private List<Path> resolvedWebRoots;

    /**
     * Web roots that are used to serve files from. File resolving is tried in the order of configurations.
     */
    private List<StaticWebRootConfiguration> webRootConfigurations;

    public FileSystemStaticHandler() {

    }

    public FileSystemStaticHandler(List<StaticWebRootConfiguration> webRootConfigurations) {
        this.webRootConfigurations = webRootConfigurations;
    }

    public List<StaticWebRootConfiguration> getWebRootConfigurations() {
        return webRootConfigurations;
    }

    public void setWebRootConfigurations(List<StaticWebRootConfiguration> webRootConfigurations) {
        this.webRootConfigurations = webRootConfigurations;
    }

    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();
        if (request.method() != HttpMethod.GET && request.method() != HttpMethod.HEAD) {
            context.next();
            return;
        }

        // decode URL path
        String uriDecodedPath = URIDecoder.decodeURIComponent(context.normalizedPath(), false);
        // if the normalized path is null it cannot be resolved
        if (uriDecodedPath == null) {
            context.next();
            return;
        }
        // will normalize and handle all paths as UNIX paths
        String path = HttpUtils.removeDots(uriDecodedPath.replace('\\', '/'));
        path = Utils.pathOffset(path, context);
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        try {
            sendStatic(context, path);
        } catch (IOException e) {
            context.fail(e);
            return;
        }
    }

    /**
     * Resolves the web roots based on the webRootConfigurations
     *
     * @throws IOException if an I/O error occurs
     */
    private void resolveWebRoots() throws IOException {
        if (resolvedWebRoots == null) {
            try {
                ROOT_CREATION_LOCK.lock();
                if (resolvedWebRoots == null) {
                    List<Path> rootPaths = new ArrayList<>();
                    for (StaticWebRootConfiguration fsConfiguration : webRootConfigurations) {
                        Path sysFSPath = Paths.get(fsConfiguration.fileSystem);
                        Path rootPath = sysFSPath;
                        if (Files.isRegularFile(sysFSPath)) {
                            FileSystem fileSystem = ZipUtils.newFileSystem(sysFSPath);
                            rootPath = fileSystem.getPath("");
                        }
                        rootPaths.add(rootPath.resolve(fsConfiguration.webRoot));
                    }
                    resolvedWebRoots = new ArrayList<>(rootPaths);
                }
            } finally {
                ROOT_CREATION_LOCK.unlock();
            }
        }
    }

    /**
     *
     * @param context
     * @param path
     * @throws IOException if an I/O error occurs
     */
    private void sendStatic(RoutingContext context, String path) throws IOException {
        resolveWebRoots();

        Path found = null;
        for (Path root : resolvedWebRoots) {
            Path resolved = root.resolve(path);
            if (resolved.getFileSystem().isOpen() && Files.exists(resolved)) {
                found = resolved;
                break;
            }
        }

        if (found == null) {
            context.next();
            return;
        }

        final HttpServerResponse response = context.response();
        String contentType = MimeMapping.getMimeTypeForFilename(path);
        if (contentType != null) {
            if (contentType.startsWith("text")) {
                response.putHeader(HttpHeaders.CONTENT_TYPE, contentType + ";charset=" + DEFAULT_CONTENT_ENCODING);
            } else {
                response.putHeader(HttpHeaders.CONTENT_TYPE, contentType);
            }
        }

        BasicFileAttributes fileAttributes = Files.readAttributes(found, BasicFileAttributes.class);
        if (fileAttributes.isDirectory()) {
            // directory listing is not supported
            context.next();
            return;
        } else if (fileAttributes.isRegularFile()) {
            context.end(Buffer.buffer(Files.readAllBytes(found)));
        }
    }

    @Override
    public void close() throws IOException {
        if (resolvedWebRoots == null) {
            // web roots are not initialized, most likely no call to dev ui was made before
            return;
        }

        // Close all filesystems that might have been created to access jar files
        for (Path resolvedWebRoot : resolvedWebRoots) {
            FileSystem fs = resolvedWebRoot.getFileSystem();

            // never attempt to close the default filesystem, e.g. WindowsFileSystem. Only one instance of it exists.
            if (fs != FileSystems.getDefault()) {
                fs.close();
            }
        }
    }

    public static class StaticWebRootConfiguration {
        /**
         * File system of the webroot. This can point to a directory on disk, or a jar file.
         */
        private String fileSystem;

        /**
         * Root directory inside the file system to service static files from. This is relative to the fileSystem. For a
         * directory fileSystem, this has to be a subdirectory. For jar files, this has to be a directory inside the jar.
         */
        private String webRoot;

        public StaticWebRootConfiguration() {

        }

        public StaticWebRootConfiguration(String fileSystemPath, String webRoot) {
            this.fileSystem = fileSystemPath;
            this.webRoot = webRoot;
        }

        public String getFileSystem() {
            return fileSystem;
        }

        public void setFileSystem(String fileSystem) {
            this.fileSystem = fileSystem;
        }

        public String getWebRoot() {
            return webRoot;
        }

        public void setWebRoot(String webRoot) {
            this.webRoot = webRoot;
        }
    }
}
