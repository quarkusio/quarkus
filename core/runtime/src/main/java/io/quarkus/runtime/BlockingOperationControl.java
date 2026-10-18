package io.quarkus.runtime;

public class BlockingOperationControl {

    private static volatile IOThreadDetector[] ioThreadDetectors;

    public static void setIoThreadDetector(IOThreadDetector[] ioThreadDetector) {
        BlockingOperationControl.ioThreadDetectors = ioThreadDetector;
    }

    public static boolean isBlockingAllowed() {
        for (IOThreadDetector ioThreadDetector : ioThreadDetectors) {
            if (ioThreadDetector.isInIOThread()) {
                return false;
            }
        }
        return true;
    }
}
