package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.IOThreadDetector;

/**
 * A build item that provides the ability to detect if the current thread is an IO thread
 */
public final class IOThreadDetectorBuildItem extends MultiBuildItem {

    private final IOThreadDetector detector;

    public IOThreadDetectorBuildItem(IOThreadDetector detector) {
        this.detector = detector;
    }

    public IOThreadDetector getDetector() {
        return detector;
    }
}
