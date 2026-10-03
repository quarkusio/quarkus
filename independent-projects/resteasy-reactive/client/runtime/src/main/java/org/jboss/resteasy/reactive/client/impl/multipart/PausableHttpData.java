package org.jboss.resteasy.reactive.client.impl.multipart;

/**
 * An {@link io.netty.handler.codec.http.multipart.HttpData} whose content is produced asynchronously, and
 * that can therefore ask the {@link PausableHttpPostRequestEncoder} to wait until enough content is available.
 */
interface PausableHttpData {

    /**
     * @param chunkSize amount of bytes
     * @return true if the requested amount of bytes is ready to be read or the content is completed, i.e. there
     *         will be no more bytes to read
     */
    boolean isReady(int chunkSize);

    /**
     * Asks the data to run the resumption action once {@code awaitedBytes} are available or the content is
     * completed.
     */
    void suspend(int awaitedBytes);
}
