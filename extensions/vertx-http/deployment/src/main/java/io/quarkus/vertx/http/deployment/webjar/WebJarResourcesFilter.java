package io.quarkus.vertx.http.deployment.webjar;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
public interface WebJarResourcesFilter {

    /**
     * Filter web jar resources. Can either update or not update the content of a web jars resource, or not include a
     * resource at all.
     *
     * @param fileName
     *        path and name of the resource inside the webjar, starting from the webJarRoot.
     * @param stream
     *        current resource content, might be null
     *
     * @return a FilterResult, never null
     */
    FilterResult apply(String fileName, InputStream stream) throws IOException;

    class FilterResult implements Closeable {
        private final InputStream stream;
        private final boolean changed;

        public FilterResult(InputStream stream, boolean changed) {
            this.stream = stream;
            this.changed = changed;
        }

        public InputStream getStream() {
            return stream;
        }

        public boolean isChanged() {
            return changed;
        }

        public boolean hasStream() {
            return stream != null;
        }

        @Override
        public void close() throws IOException {
            if (hasStream()) {
                stream.close();
            }
        }
    }
}
