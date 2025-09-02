package org.jboss.resteasy.reactive.server.core;

/**
 * @deprecated This class has been replaced by {@link org.jboss.resteasy.reactive.common.core.BlockingOperationSupport}
 */
@Deprecated(forRemoval = true, since = "3.27")
public class BlockingOperationSupport {

    private static volatile IOThreadDetector ioThreadDetector;

    //TODO: move away from a static
    public static void setIoThreadDetector(IOThreadDetector ioThreadDetector) {
        BlockingOperationSupport.ioThreadDetector = ioThreadDetector;
    }

    public static boolean isBlockingAllowed() {
        if (ioThreadDetector == null) {
            return true;
        }
        return ioThreadDetector.isBlockingAllowed();
    }

    public interface IOThreadDetector {

        boolean isBlockingAllowed();
    }

}
