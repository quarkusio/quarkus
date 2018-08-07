package org.jboss.shamrock.undertow.runtime.graal;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * TODO: this is pretty horrible
 * <p>
 * This class is a bit of a hack to allow static resource to work correctly with Substrate.
 * <p>
 * The class path is iterated at image build time, and any web resources have their content
 * length and last modified times stored in a Map.
 * <p>
 * This seems like a simple enough solution for now, but hopefully we can do something better in the future.
 *
 * Another possibility is just not supporting embedded resources, and requiring resources to be served from
 * the file system (or extracted tmp dir)
 */
@TargetClass(className = "io.undertow.server.handlers.resource.URLResource")
final class URLResourceSubstitution {

    @Alias
    private boolean connectionOpened = false;
    @Alias
    private Date lastModified;
    @Alias
    private Long contentLength;

    @Alias
    private String path;


    @Substitute
    private void openConnection() {
        if (!connectionOpened) {
            connectionOpened = true;
            ResourceInfo res = ResourceInfo.RESOURCES.get(path.startsWith("/") ? path : ("/" + path));
            if (res != null) {
                contentLength = res.contentLength;
                lastModified = new Date(res.lastModified);
            }
        }
    }


    static final class ResourceInfo {

        private static final String META_INF_RESOURCES = "META-INF/resources";
        static final Map<String, ResourceInfo> RESOURCES;

        private final long lastModified;
        private final long contentLength;

        ResourceInfo(long lastModified, long contentLength) {
            this.lastModified = lastModified;
            this.contentLength = contentLength;
        }

        static {

            ClassLoader cl = URLResourceSubstitution.class.getClassLoader();
            Map<String, ResourceInfo> map = new HashMap<>();
            URLClassLoader ucl = (URLClassLoader) cl;
            for (URL res : ucl.getURLs()) {
                if (res.getProtocol().equals("file") && res.getPath().endsWith(".jar")) {
                    try (JarFile file = new JarFile(res.getPath())) {
                        Enumeration<JarEntry> e = file.entries();
                        while (e.hasMoreElements()) {
                            JarEntry entry = e.nextElement();
                            if (entry.getName().startsWith(META_INF_RESOURCES)) {
                                map.put(entry.getName().substring(META_INF_RESOURCES.length()), new ResourceInfo(entry.getLastModifiedTime().toMillis(), entry.getSize()));
                            }
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            RESOURCES = Collections.unmodifiableMap(map);
        }

        @Override
        public String toString() {
            return "ResourceInfo{" +
                    "lastModified=" + lastModified +
                    ", contentLength=" + contentLength +
                    '}';
        }
    }

}
