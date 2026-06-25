package io.quarkus.vertx.core.runtime.config;

/**
 * An enum storing the different Native transports.
 * <p>
 * Except if you want to force on a specific native transport, use {@code AUTO}
 */
public enum NativeTransportType {
    /**
     * Pick the most appropriate one.
     * This is the default.
     */
    AUTO,
    /**
     * EPOLL (Linux Only)
     * <p>
     * <a href="https://man7.org/linux/man-pages/man7/epoll.7.html">See epoll man page</a>.
     */
    EPOLL,

    /**
     * KQUEUE (OSX only)
     * <p>
     * <a href="https://en.wikipedia.org/wiki/Kqueue">See KQueue on Wikipedia</a>
     */
    KQUEUE,

    /**
     * IO_URING (Linux only, need a recent Linux kernel (5.10+)).
     * <p>
     * <a href="https://man7.org/linux/man-pages/man7/io_uring.7.html">See io_uring man page</a>.
     */
    IO_URING
}
