package io.quarkus.vertx.http.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Request body related settings
 */
@ConfigGroup
public class BodyConfig {

    /**
     * Whether the files sent using {@code multipart/form-data} will be stored locally.
     * <p>
     * If {@code true}, they will be stored in {@code quarkus.http.body-handler.uploads-directory} and will be made
     * available via {@code io.vertx.ext.web.RoutingContext.fileUploads()}. Otherwise, the the files sent using
     * {@code multipart/form-data} will not be stored locally, and {@code io.vertx.ext.web.RoutingContext.fileUploads()}
     * will always return an empty collection. Note that even with this option being set to {@code false}, the
     * {@code multipart/form-data} requests will be accepted.
     */
    @ConfigItem(defaultValue = "true")
    public boolean handleFileUploads;

    /**
     * The directory where the files sent using {@code multipart/form-data} should be stored.
     * <p>
     * Either an absolute path or a path relative to the current directory of the application process.
     */
    @ConfigItem(defaultValue = "file-uploads")
    public String uploadsDirectory;

    /**
     * Whether the form attributes should be added to the request parameters.
     * <p>
     * If {@code true}, the form attributes will be added to the request parameters; otherwise the form parameters will
     * not be added to the request parameters
     */
    @ConfigItem(defaultValue = "true")
    public boolean mergeFormAttributes;

    /**
     * Whether the uploaded files should be removed after serving the request.
     * <p>
     * If {@code true} the uploaded files stored in {@code quarkus.http.body-handler.uploads-directory} will be removed
     * after handling the request. Otherwise the files will be left there forever.
     */
    @ConfigItem(defaultValue = "false")
    public boolean deleteUploadedFilesOnEnd;

    /**
     * Whether the body buffer should pre-allocated based on the {@code Content-Length} header value.
     * <p>
     * If {@code true} the body buffer is pre-allocated according to the size read from the {@code Content-Length}
     * header. Otherwise the body buffer is pre-allocated to 1KB, and is resized dynamically
     */
    @ConfigItem(defaultValue = "false")
    public boolean preallocateBodyBuffer;
}