package org.jboss.resteasy.reactive.client.impl.multipart;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.multipart.AbstractHttpData;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.internal.ObjectUtil;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.impl.buffer.VertxByteBufAllocator;

/**
 * A FileUpload implementation that is responsible for sending an {@link InputStream} as a file in a multipart
 * message, reading it in chunks on a worker thread so that the stream is never read on the event loop.
 * It is meant to be used by the {@link PausableHttpPostRequestEncoder}, in the same way as {@link MultiByteHttpData}.
 *
 * When created, InputStreamHttpData starts filling its buffer from the stream. Before reading the next chunk of
 * data with {@link #getChunk(int)}, the post encoder checks if data {@link #isReady(int)} and if not, triggers
 * {@link #suspend(int)}. That's because a chunk smaller than requested is treated as the end of input.
 * Then, when the requested amount of bytes is ready, or the stream is exhausted, `resumption` is executed.
 */
public class InputStreamHttpData extends AbstractHttpData implements FileUpload, PausableHttpData {

    private final InputStream inputStream;
    private final Consumer<Throwable> errorHandler;
    private final Context context;
    private final Runnable resumption;
    private final ByteBuf buffer;

    private String filename;
    private String contentType;
    private String contentTransferEncoding;

    // all the fields below are only accessed on the Vert.x context
    private boolean done = false;
    private boolean reading = false;
    private boolean paused = false;
    private int awaitedBytes;

    public InputStreamHttpData(String name, String filename, String contentType,
            String contentTransferEncoding, Charset charset, InputStream inputStream,
            Consumer<Throwable> errorHandler, Context context, Runnable resumption, int bufferSize) {
        super(name, charset, 0);
        this.inputStream = inputStream;
        this.errorHandler = errorHandler;
        this.context = context;
        this.resumption = resumption;
        this.buffer = VertxByteBufAllocator.DEFAULT.heapBuffer(bufferSize, bufferSize);
        setFilename(filename);
        setContentType(contentType);
        setContentTransferEncoding(contentTransferEncoding);
        context.runOnContext(v -> fill());
    }

    /**
     * Reads the next chunk from the stream on a worker thread, and keeps doing so until the buffer is full or the
     * stream is exhausted.
     */
    private void fill() {
        if (done || reading) {
            return;
        }
        int toRead = buffer.writableBytes();
        if (toRead == 0) {
            return;
        }
        reading = true;
        context.executeBlocking(() -> {
            byte[] bytes = new byte[toRead];
            int amount = inputStream.read(bytes);
            if (amount == -1) {
                inputStream.close();
                return null;
            }
            return Unpooled.wrappedBuffer(bytes, 0, amount);
        }, true).onComplete(ar -> {
            reading = false;
            if (ar.succeeded()) {
                ByteBuf chunk = ar.result();
                if (chunk == null) {
                    done = true;
                } else {
                    buffer.writeBytes(chunk);
                }
            } else {
                done = true;
                closeQuietly();
                errorHandler.accept(ar.cause());
            }
            if (paused && (done || buffer.readableBytes() >= awaitedBytes)) {
                paused = false;
                awaitedBytes = 0;
                resumption.run();
            }
            fill();
        });
    }

    private void closeQuietly() {
        try {
            inputStream.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public void suspend(int awaitedBytes) {
        this.awaitedBytes = awaitedBytes;
        this.paused = true;
    }

    @Override
    public boolean isReady(int chunkSize) {
        return done || buffer.readableBytes() >= chunkSize;
    }

    /**
     * {@inheritDoc}
     * <br/>
     * NOTE: should only be invoked when {@link #isReady(int)} returns true
     *
     * @param toRead amount of bytes to read
     * @return ByteBuf with the requested bytes
     */
    @Override
    public ByteBuf getChunk(int toRead) {
        if (Vertx.currentContext() != context) {
            throw new IllegalStateException("InputStreamHttpData invoked on an invalid context : " + Vertx.currentContext()
                    + ", thread: " + Thread.currentThread());
        }
        if (buffer.readableBytes() == 0 && done) {
            return Unpooled.EMPTY_BUFFER;
        }
        int readBytes = Math.min(buffer.readableBytes(), toRead);
        ByteBuf result = VertxByteBufAllocator.DEFAULT.heapBuffer(readBytes, readBytes);
        result.writeBytes(buffer, readBytes);
        buffer.discardReadBytes();
        fill();
        return result;
    }

    @Override
    public void setContent(ByteBuf buffer) throws IOException {
        throw new IllegalStateException("setting content of InputStreamHttpData is not supported");
    }

    @Override
    public void addContent(ByteBuf buffer, boolean last) throws IOException {
        throw new IllegalStateException("adding content to InputStreamHttpData is not supported");
    }

    @Override
    public void setContent(File file) throws IOException {
        throw new IllegalStateException("setting content of InputStreamHttpData is not supported");
    }

    @Override
    public void setContent(InputStream inputStream) throws IOException {
        throw new IllegalStateException("setting content of InputStreamHttpData is not supported");
    }

    @Override
    public void delete() {
        // do nothing
    }

    @Override
    public byte[] get() throws IOException {
        throw new IllegalStateException("getting all the contents of an InputStreamHttpData is not supported");
    }

    @Override
    public ByteBuf getByteBuf() {
        throw new IllegalStateException("getting all the contents of an InputStreamHttpData is not supported");
    }

    @Override
    public String getString() {
        throw new IllegalStateException("Reading InputStreamHttpData as String is not supported");
    }

    @Override
    public String getString(Charset encoding) {
        throw new IllegalStateException("Reading InputStreamHttpData as String is not supported");
    }

    @Override
    public boolean renameTo(File dest) {
        throw new IllegalStateException("Renaming destination file for InputStreamHttpData is not supported");
    }

    @Override
    public boolean isInMemory() {
        return true;
    }

    @Override
    public File getFile() {
        return null;
    }

    @Override
    public FileUpload copy() {
        throw new IllegalStateException("Copying InputStreamHttpData is not supported");
    }

    @Override
    public FileUpload duplicate() {
        throw new IllegalStateException("Duplicating InputStreamHttpData is not supported");
    }

    @Override
    public FileUpload retainedDuplicate() {
        throw new IllegalStateException("Duplicating InputStreamHttpData is not supported");
    }

    @Override
    public FileUpload replace(ByteBuf content) {
        throw new IllegalStateException("Replacing InputStreamHttpData is not supported");
    }

    @Override
    public FileUpload retain(int increment) {
        super.retain(increment);
        return this;
    }

    @Override
    public FileUpload retain() {
        super.retain();
        return this;
    }

    @Override
    public FileUpload touch() {
        touch(null);
        return this;
    }

    @Override
    public FileUpload touch(Object hint) {
        buffer.touch(hint);
        return this;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        return System.identityHashCode(this) == System.identityHashCode(o);
    }

    @Override
    public int compareTo(InterfaceHttpData o) {
        if (!(o instanceof InputStreamHttpData)) {
            throw new ClassCastException("Cannot compare " + getHttpDataType() +
                    " with " + o.getHttpDataType());
        }
        return Integer.compare(System.identityHashCode(this), System.identityHashCode(o));
    }

    @Override
    public HttpDataType getHttpDataType() {
        return HttpDataType.FileUpload;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public void setFilename(String filename) {
        this.filename = ObjectUtil.checkNotNull(filename, "filename");
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = ObjectUtil.checkNotNull(contentType, "contentType");
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public String getContentTransferEncoding() {
        return contentTransferEncoding;
    }

    @Override
    public void setContentTransferEncoding(String contentTransferEncoding) {
        this.contentTransferEncoding = contentTransferEncoding;
    }

    @Override
    public long length() {
        return buffer.readableBytes() + super.length();
    }

    @Override
    public String toString() {
        return HttpHeaderNames.CONTENT_DISPOSITION + ": " +
                HttpHeaderValues.FORM_DATA + "; " + HttpHeaderValues.NAME + "=\"" + getName() +
                "\"; " + HttpHeaderValues.FILENAME + "=\"" + filename + "\"\r\n" +
                HttpHeaderNames.CONTENT_TYPE + ": " + contentType +
                (getCharset() != null ? "; " + HttpHeaderValues.CHARSET + '=' + getCharset().name() + "\r\n" : "\r\n") +
                HttpHeaderNames.CONTENT_LENGTH + ": " + length() + "\r\n" +
                "Completed: " + isCompleted();
    }
}
