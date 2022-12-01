package io.quarkus.undertow.runtime;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import io.undertow.httpcore.OutputChannel;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.util.ETag;
import io.undertow.util.MimeMappings;

public class KnownPathResourceManager implements ResourceManager {

    private final NavigableSet<String> files;
    private final Set<String> directories;
    private final ResourceManager underlying;

    public KnownPathResourceManager(Set<String> files, Set<String> directories, ResourceManager underlying) {
        this.underlying = underlying;
        TreeSet<String> tree = new TreeSet<>();
        for (String i : files) {
            if (i.startsWith("/")) {
                i = i.substring(1);
            }
            if (i.endsWith("/")) {
                i = i.substring(0, i.length() - 1);
            }
            tree.add(i);
        }
        this.files = tree;
        Set<String> tmp = new HashSet<>();
        for (String i : directories) {
            if (i.startsWith("/")) {
                i = i.substring(1);
            }
            if (i.endsWith("/")) {
                i = i.substring(0, i.length() - 1);
            }
            tmp.add(i);
        }
        tmp.add("");
        this.directories = Collections.unmodifiableSet(tmp);
    }

    @Override
    public Resource getResource(String path) throws IOException {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (directories.contains(path)) {
            return new DirectoryResource(path);
        }
        return underlying.getResource(path);
    }

    @Override
    public void close() throws IOException {
        underlying.close();
    }

    private class DirectoryResource implements Resource {

        private final String path;

        private DirectoryResource(String path) {
            this.path = path;
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public Date getLastModified() {
            return null;
        }

        @Override
        public String getLastModifiedString() {
            return null;
        }

        @Override
        public ETag getETag() {
            return null;
        }

        @Override
        public String getName() {
            int i = path.lastIndexOf('/');
            if (i == -1) {
                return path;
            }
            return path.substring(i + 1);
        }

        @Override
        public boolean isDirectory() {
            return true;
        }

        @Override
        public List<Resource> list() {
            List<Resource> ret = new ArrayList<>();
            String slashPath = path + "/";
            for (String i : files.headSet(path)) {
                if (i.startsWith(slashPath)) {
                    try {
                        ret.add(underlying.getResource(i));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    break;
                }
            }
            return ret;
        }

        @Override
        public String getContentType(MimeMappings mimeMappings) {
            return null;
        }

        @Override
        public void serveBlocking(OutputStream outputStream, HttpServerExchange exchange) throws IOException {
            throw new IOException("Cannot serve directory");
        }

        @Override
        public void serveAsync(OutputChannel stream, HttpServerExchange exchange) {
            exchange.setStatusCode(500);
            exchange.endExchange();
        }

        @Override
        public Long getContentLength() {
            return null;
        }

        @Override
        public String getCacheKey() {
            return null;
        }

        @Override
        public File getFile() {
            return null;
        }

        @Override
        public Path getFilePath() {
            return null;
        }

        @Override
        public File getResourceManagerRoot() {
            return null;
        }

        @Override
        public Path getResourceManagerRootPath() {
            return null;
        }

        @Override
        public URL getUrl() {
            return null;
        }
    }
}
