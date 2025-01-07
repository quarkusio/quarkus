package io.quarkus.runtime;

import java.util.stream.Stream;

public class BlockingOperationControl {

    private static volatile IOThreadDetector[] ioThreadDetectors;

    public static void setIoThreadDetector(IOThreadDetector[] ioThreadDetector) {
        ioThreadDetectors = ioThreadDetector;
    }

    public static boolean isBlockingAllowed() {
        return Stream.of(ioThreadDetectors).noneMatch(IOThreadDetector::isInIOThread);
    }

}
