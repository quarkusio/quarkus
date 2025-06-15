/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.jboss.resteasy.reactive.client.impl.multipart;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow.Subscription;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.multipart.AbstractHttpData;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.internal.ObjectUtil;
import io.smallrye.mutiny.Multi;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.impl.VertxByteBufAllocator;

/**
 * A FileUpload implementation that is responsible for sending Multi&lt;Byte&gt; as a file in a multipart message. It is
 * meant to be used by the {@link PausableHttpPostRequestEncoder} When created, MultiByteHttpData will subscribe to the
 * underlying Multi and request {@link MultiByteHttpData#BUFFER_SIZE} of bytes. Before reading the next chunk of data
 * with {@link #getChunk(int)}, the post encoder checks if data {@link #isReady(int)} and if not, triggers
 * {@link #suspend(int)}. That's because a chunk smaller than requested is treated as the end of input. Then, when the
 * requested amount of bytes is ready, or the underlying Multi is completed, `resumption` is executed.
 */
public class MultiByteHttpData extends AbstractHttpData implements FileUpload {
    private static final Logger log = Logger.getLogger(MultiByteHttpData.class);

    public static final int DEFAULT_BUFFER_SIZE = 16384;
    private static final int BUFFER_SIZE;

    private Subscription subscription;
    private String filename;

    private String contentType;

    private String contentTransferEncoding;

    // TODO: replace with a simple array based?
    // TODO: we do `discardReadBytes` on it which is not optimal - copies array every time
    private final ByteBuf buffer = VertxByteBufAllocator.DEFAULT.heapBuffer(BUFFER_SIZE, BUFFER_SIZE);

    private final Context context;

    private volatile boolean done = false;

    private boolean paused = false;
    private int awaitedBytes;

    static {
        BUFFER_SIZE = Integer.parseInt(
                System.getProperty("quarkus.rest.client.multipart-buffer-size", String.valueOf(DEFAULT_BUFFER_SIZE)));
        if (BUFFER_SIZE < DEFAULT_BUFFER_SIZE) {
            throw new IllegalStateException(
                    "quarkus.rest.client.multipart-buffer-size cannot be lower than " + DEFAULT_BUFFER_SIZE);
        }
    }

    /**
     * @param name
     *        name of the parameter
     * @param filename
     *        file name
     * @param contentType
     *        content type
     * @param contentTransferEncoding
     *        "binary" for sending binary files
     * @param charset
     *        the charset
     * @param content
     *        the Multi to send
     * @param errorHandler
     *        error handler invoked when the Multi emits an exception
     * @param context
     *        Vertx context on which the data is sent
     * @param resumption
     *        the action to execute when the requested amount of bytes is ready, or the Multi is completed
     */
    public MultiByteHttpData(String name, String filename, String contentType, String contentTransferEncoding,
            Charset charset, Multi<Byte> content, Consumer<Throwable> errorHandler, Context context,
            Runnable resumption) {
        super(name, charset, 0);
        this.context = context;
        setFilename(filename);
        setContentType(contentType);
        setContentTransferEncoding(contentTransferEncoding);

        var contextualExecutor = new ExecutorWithContext(context);
        content.emitOn(contextualExecutor).runSubscriptionOn(contextualExecutor).subscribe().with(subscription -> {
            MultiByteHttpData.this.subscription = subscription;
            subscription.request(BUFFER_SIZE);
        }, b -> {
            buffer.writeByte(b);
            if (paused && (done || buffer.readableBytes() >= awaitedBytes)) {
                paused = false;
                awaitedBytes = 0;
                resumption.run();
            }
        }, th -> {
            log.error("Multi<Byte> used to send a multipart message failed", th);
            done = true;
            errorHandler.accept(th);
        }, () -> {
            done = true;
            if (paused) {
                paused = false;
                resumption.run();
            }
        });
    }

    void suspend(int awaitedBytes) {
        this.awaitedBytes = awaitedBytes;
        this.paused = true;
    }

    @Override
    public void setContent(ByteBuf buffer) throws IOException {
        throw new IllegalStateException("setting content of MultiByteHttpData is not supported");
    }

    @Override
    public void addContent(ByteBuf buffer, boolean last) throws IOException {
        throw new IllegalStateException("adding content to MultiByteHttpData is not supported");
    }

    @Override
    public void setContent(File file) throws IOException {
        throw new IllegalStateException("setting content of MultiByteHttpData is not supported");
    }

    @Override
    public void setContent(InputStream inputStream) throws IOException {
        throw new IllegalStateException("setting content of MultiByteHttpData is not supported");
    }

    @Override
    public void delete() {
        // do nothing
    }

    @Override
    public byte[] get() throws IOException {
        throw new IllegalStateException("getting all the contents of a MultiByteHttpData is not supported");
    }

    @Override
    public ByteBuf getByteBuf() {
        throw new IllegalStateException("getting all the contents of a MultiByteHttpData is not supported");
    }

    /**
     * check if it is possible to read the next chunk of data of a given size
     *
     * @param chunkSize
     *        amount of bytes
     *
     * @return true if the requested amount of bytes is ready to be read or the Multi is completed, i.e. there will be
     *         no more bytes to read
     */
    public boolean isReady(int chunkSize) {
        return done || buffer.readableBytes() >= chunkSize;
    }

    /**
     * {@inheritDoc} <br/>
     * NOTE: should only be invoked when {@link #isReady(int)} returns true
     *
     * @param toRead
     *        amount of bytes to read
     *
     * @return ByteBuf with the requested bytes
     */
    @Override
    public ByteBuf getChunk(int toRead) {
        if (Vertx.currentContext() != context) {
            throw new IllegalStateException("MultiByteHttpData invoked on an invalid context : "
                    + Vertx.currentContext() + ", thread: " + Thread.currentThread());
        }
        if (buffer.readableBytes() == 0 && done) {
            return Unpooled.EMPTY_BUFFER;
        }

        ByteBuf result = VertxByteBufAllocator.DEFAULT.heapBuffer(toRead, toRead);

        // finish if the whole buffer is filled
        // or we hit the end, `done` && buffer.readableBytes == 0
        while (toRead > 0 && !(buffer.readableBytes() == 0 && done)) {
            int readBytes = Math.min(buffer.readableBytes(), toRead);
            result.writeBytes(buffer.readBytes(readBytes));
            buffer.discardReadBytes();
            subscription.request(readBytes);

            toRead -= readBytes;
        }
        return result;
    }

    @Override
    public String getString() {
        throw new IllegalStateException("Reading MultiByteHttpData as String is not supported");
    }

    @Override
    public String getString(Charset encoding) {
        throw new IllegalStateException("Reading MultiByteHttpData as String is not supported");
    }

    @Override
    public boolean renameTo(File dest) {
        throw new IllegalStateException("Renaming destination file for MultiByteHttpData is not supported");
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
        throw new IllegalStateException("Copying MultiByteHttpData is not supported");
    }

    @Override
    public FileUpload duplicate() {
        throw new IllegalStateException("Duplicating MultiByteHttpData is not supported");
    }

    @Override
    public FileUpload retainedDuplicate() {
        throw new IllegalStateException("Duplicating MultiByteHttpData is not supported");
    }

    @Override
    public FileUpload replace(ByteBuf content) {
        throw new IllegalStateException("Replacing MultiByteHttpData is not supported");
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
        if (!(o instanceof MultiByteHttpData)) {
            throw new ClassCastException("Cannot compare " + getHttpDataType() + " with " + o.getHttpDataType());
        }
        return compareTo((MultiByteHttpData) o);
    }

    public int compareTo(MultiByteHttpData o) {
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
        return HttpHeaderNames.CONTENT_DISPOSITION + ": " + HttpHeaderValues.FORM_DATA + "; " + HttpHeaderValues.NAME
                + "=\"" + getName() + "\"; " + HttpHeaderValues.FILENAME + "=\"" + filename + "\"\r\n"
                + HttpHeaderNames.CONTENT_TYPE + ": " + contentType
                + (getCharset() != null ? "; " + HttpHeaderValues.CHARSET + '=' + getCharset().name() + "\r\n" : "\r\n")
                + HttpHeaderNames.CONTENT_LENGTH + ": " + length() + "\r\n" + "Completed: " + isCompleted();
    }

    static class ExecutorWithContext implements Executor {
        Context context;

        public ExecutorWithContext(Context context) {
            this.context = context;
        }

        @Override
        public void execute(Runnable command) {
            if (Vertx.currentContext() == context) {
                command.run();
            } else {
                context.runOnContext(v -> command.run());
            }
        }
    }
}
