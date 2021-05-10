package io.quarkus.resteasy.reactive.server.runtime;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.RuntimeConfigurableServerRestHandler;
import org.jboss.resteasy.reactive.server.spi.RuntimeConfiguration;

import io.netty.handler.codec.DecoderException;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.FileUploadImpl;

/**
 * ServerRestHandler implementation that handles the {@code multipart/form-data} media type.
 *
 * The code has been adapted from {@link io.vertx.ext.web.handler.impl.BodyHandlerImpl}
 * and its main functionality is to populate {@code RoutingContext}'s fileUploads
 */
public class MultipartFormHandler implements RuntimeConfigurableServerRestHandler {

    private static final Logger LOG = Logger.getLogger(MultipartFormHandler.class);

    // in multipart requests, the body should not be available as a stream
    private static final ByteArrayInputStream NO_BYTES_INPUT_STREAM = new ByteArrayInputStream(new byte[0]);

    private volatile String uploadsDirectory;
    private volatile boolean deleteUploadedFilesOnEnd;
    private volatile Optional<Long> maxBodySize;
    private volatile ClassLoader tccl;

    @Override
    public void configure(RuntimeConfiguration configuration) {
        uploadsDirectory = configuration.body().uploadsDirectory();
        deleteUploadedFilesOnEnd = configuration.body().deleteUploadedFilesOnEnd();
        maxBodySize = configuration.limits().maxBodySize();
        // capture the proper TCCL in order to avoid losing it to Vert.x in dev-mode
        tccl = Thread.currentThread().getContextClassLoader();

        try {
            Files.createDirectories(Paths.get(uploadsDirectory));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void handle(ResteasyReactiveRequestContext context) throws Exception {
        // in some cases, with sub-resource locators or via request filters,
        // it's possible we've already read the entity
        if (context.hasInputStream()) {
            // let's not set it twice
            return;
        }
        if (context.serverRequest().getRequestMethod().equals(HttpMethod.GET) ||
                context.serverRequest().getRequestMethod().equals(HttpMethod.HEAD)) {
            return;
        }
        HttpServerRequest httpServerRequest = context.serverRequest().unwrap(HttpServerRequest.class);
        if (httpServerRequest.isEnded()) {
            context.setInputStream(NO_BYTES_INPUT_STREAM);
        } else {
            httpServerRequest.setExpectMultipart(true);
            httpServerRequest.pause();
            context.suspend();
            MultipartFormVertxHandler handler = new MultipartFormVertxHandler(context, tccl, uploadsDirectory,
                    deleteUploadedFilesOnEnd, maxBodySize);
            httpServerRequest.handler(handler);
            httpServerRequest.endHandler(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    handler.end();
                }
            });
            httpServerRequest.resume();
        }
    }

    private static class MultipartFormVertxHandler implements Handler<Buffer> {
        private final ResteasyReactiveRequestContext rrContext;
        private final RoutingContext context;
        private final ClassLoader tccl;

        private final String uploadsDirectory;
        private final boolean deleteUploadedFilesOnEnd;
        private final Optional<Long> maxBodySize;

        boolean failed;
        AtomicInteger uploadCount = new AtomicInteger();
        AtomicBoolean cleanup = new AtomicBoolean(false);
        boolean ended;
        long uploadSize = 0L;

        public MultipartFormVertxHandler(ResteasyReactiveRequestContext rrContext, ClassLoader tccl, String uploadsDirectory,
                boolean deleteUploadedFilesOnEnd, Optional<Long> maxBodySize) {
            this.rrContext = rrContext;
            this.context = rrContext.serverRequest().unwrap(RoutingContext.class);
            this.tccl = tccl;
            this.uploadsDirectory = uploadsDirectory;
            this.deleteUploadedFilesOnEnd = deleteUploadedFilesOnEnd;
            this.maxBodySize = maxBodySize;
            Set<FileUpload> fileUploads = context.fileUploads();

            context.request().setExpectMultipart(true);
            context.request().exceptionHandler(new Handler<Throwable>() {
                @Override
                public void handle(Throwable t) {
                    cancelUploads();
                    rrContext.resume(new WebApplicationException(
                            (t instanceof DecoderException) ? Response.Status.REQUEST_ENTITY_TOO_LARGE
                                    : Response.Status.INTERNAL_SERVER_ERROR));
                }
            });
            context.request().uploadHandler(new Handler<HttpServerFileUpload>() {
                @Override
                public void handle(HttpServerFileUpload upload) {
                    if (maxBodySize.isPresent() && upload.isSizeAvailable()) {
                        // we can try to abort even before the upload starts
                        long size = uploadSize + upload.size();
                        if (size > MultipartFormVertxHandler.this.maxBodySize.get()) {
                            failed = true;
                            restoreProperTCCL();
                            rrContext.resume(new WebApplicationException(Response.Status.REQUEST_ENTITY_TOO_LARGE));
                            return;
                        }
                    }
                    // we actually upload to a file with a generated filename
                    uploadCount.incrementAndGet();
                    String uploadedFileName = new File(MultipartFormVertxHandler.this.uploadsDirectory,
                            UUID.randomUUID().toString()).getPath();
                    upload.exceptionHandler(new UploadExceptionHandler(rrContext));
                    upload.streamToFileSystem(uploadedFileName)
                            .onSuccess(new Handler<Void>() {
                                @Override
                                public void handle(Void x) {
                                    uploadEnded();
                                }
                            })
                            .onFailure(new Handler<Throwable>() {
                                @Override
                                public void handle(Throwable ignored) {
                                    new UploadExceptionHandler(rrContext);
                                }
                            });
                    FileUploadImpl fileUpload = new FileUploadImpl(uploadedFileName, upload);
                    fileUploads.add(fileUpload);
                }
            });
        }

        private void cancelUploads() {
            for (FileUpload fileUpload : context.fileUploads()) {
                FileSystem fileSystem = context.vertx().fileSystem();
                try {
                    if (!fileUpload.cancel()) {
                        String uploadedFileName = fileUpload.uploadedFileName();
                        fileSystem.delete(uploadedFileName, deleteResult -> {
                            if (deleteResult.failed()) {
                                LOG.warn("Delete of uploaded file failed: " + uploadedFileName, deleteResult.cause());
                            }
                        });
                    }
                } catch (Exception e) {
                    LOG.debug("Unable to cancel file upload:", e);
                }
            }
        }

        @Override
        public void handle(Buffer buff) {
            if (failed) {
                return;
            }
            uploadSize += buff.length();
            if (maxBodySize.isPresent() && uploadSize > maxBodySize.get()) {
                failed = true;
                // enqueue a delete for the error uploads
                context.vertx().runOnContext(new Handler<Void>() {
                    @Override
                    public void handle(Void v) {
                        MultipartFormVertxHandler.this.deleteFileUploads();
                    }
                });
                restoreProperTCCL();
                rrContext.resume(new WebApplicationException(Response.Status.REQUEST_ENTITY_TOO_LARGE));
            }
        }

        private void restoreProperTCCL() {
            Thread.currentThread().setContextClassLoader(tccl);
        }

        void uploadEnded() {
            int count = uploadCount.decrementAndGet();
            // only if parsing is done and count is 0 then all files have been processed
            if (ended && count == 0) {
                doEnd();
            }
        }

        void end() {
            // this marks the end of body parsing, calling doEnd should
            // only be possible from this moment onwards
            ended = true;
            // only if parsing is done and count is 0 then all files have been processed
            if (uploadCount.get() == 0) {
                doEnd();
            }
        }

        void doEnd() {
            if (failed) {
                deleteFileUploads();
                return;
            }
            if (deleteUploadedFilesOnEnd) {
                context.addBodyEndHandler(x -> deleteFileUploads());
            }
            rrContext.setInputStream(NO_BYTES_INPUT_STREAM);
            restoreProperTCCL();
            rrContext.resume();
        }

        private void deleteFileUploads() {
            if (cleanup.compareAndSet(false, true)) {
                for (FileUpload fileUpload : context.fileUploads()) {
                    FileSystem fileSystem = context.vertx().fileSystem();
                    String uploadedFileName = fileUpload.uploadedFileName();
                    fileSystem.exists(uploadedFileName, existResult -> {
                        if (existResult.failed()) {
                            LOG.warn("Could not detect if uploaded file exists, not deleting: " + uploadedFileName,
                                    existResult.cause());
                        } else if (existResult.result()) {
                            fileSystem.delete(uploadedFileName, deleteResult -> {
                                if (deleteResult.failed()) {
                                    LOG.warn("Delete of uploaded file failed: " + uploadedFileName, deleteResult.cause());
                                }
                            });
                        }
                    });
                }
            }
        }

        private class UploadExceptionHandler implements Handler<Throwable> {
            private final ResteasyReactiveRequestContext rrContext;

            public UploadExceptionHandler(ResteasyReactiveRequestContext rrContext) {
                this.rrContext = rrContext;
            }

            @Override
            public void handle(Throwable t) {
                MultipartFormVertxHandler.this.deleteFileUploads();
                rrContext.resume(new WebApplicationException(t, Response.Status.INTERNAL_SERVER_ERROR));
            }
        }
    }
}
