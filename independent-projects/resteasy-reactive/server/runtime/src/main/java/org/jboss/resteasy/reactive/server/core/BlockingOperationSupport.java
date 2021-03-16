package org.jboss.resteasy.reactive.server.core;

public class BlockingOperationSupport {

    private static volatile IOThreadDetector ioThreadDetector;

    public static void setIoThreadDetector(IOThreadDetector ioThreadDetector) {
        BlockingOperationSupport.ioThreadDetector = ioThreadDetector;
    }

    public static boolean isBlockingAllowed() {
        return ioThreadDetector.isBlockingAllowed();
    }

    public interface IOThreadDetector {

        boolean isBlockingAllowed();
    }

}
