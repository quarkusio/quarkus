package io.quarkus.deployment.builditem;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.builder.item.SimpleBuildItem;

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
