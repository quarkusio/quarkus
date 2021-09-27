package io.quarkus.runtime;

public class BlockingOperationControl {

    private static volatile IOThreadDetector[] ioThreadDetectors;

    public static void setIoThreadDetector(IOThreadDetector[] ioThreadDetector) {
        BlockingOperationControl.ioThreadDetectors = ioThreadDetector;
    }

    public static boolean isBlockingAllowed() {
        for (int i = 0; i < ioThreadDetectors.length; ++i) {
            if (ioThreadDetectors[i].isInIOThread()) {
                return false;
            }
        }
        return true;
    }
}
