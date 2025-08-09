package io.quarkus.deployment.builditem;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Represents a build item for managing {@link Closeable} resources during the build
 * <p>
 * This build item collects {@link Closeable} resources that need to be closed
 * <p>
 * It provides a central place to register closeable resources (like file systems)
 * and ensures they are all closed at the end of the build.
 */
public final class QuarkusBuildCloseablesBuildItem extends SimpleBuildItem implements Closeable {

    private static final Logger log = Logger.getLogger(QuarkusBuildCloseablesBuildItem.class);

    private final List<Closeable> closeables = Collections.synchronizedList(new ArrayList<>());

    public <T extends Closeable> T add(T closeable) {
        closeables.add(closeable);
        return closeable;
    }

    @Override
    public void close() {
        for (Closeable c : closeables) {
            try {
                c.close();
            } catch (Throwable e) {
                log.debugf(e, "Failed to close %s", c);
            }
        }
    }
}
