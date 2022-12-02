package io.quarkus.dev.spi;

public enum DevModeType {
    LOCAL(true),
    REMOTE_LOCAL_SIDE(false),
    REMOTE_SERVER_SIDE(false),
    TEST_ONLY(true);

    final boolean continuousTestingSupported;

    DevModeType(boolean continuousTestingSupported) {
        this.continuousTestingSupported = continuousTestingSupported;
    }

    public boolean isContinuousTestingSupported() {
        return continuousTestingSupported;
    }
}
