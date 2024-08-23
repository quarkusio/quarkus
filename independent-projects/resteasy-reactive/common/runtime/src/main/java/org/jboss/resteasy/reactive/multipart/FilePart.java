package org.jboss.resteasy.reactive.multipart;

import java.nio.file.Path;

/**
 * Represents a file-part (upload or download) from an HTTP multipart form submission.
 */
public interface FilePart {

    /**
     * @return the name of the upload as provided in the form submission.
     */
    String name();

    /**
     * @return the actual temporary file name on the server where the file was uploaded to.
     */
    Path filePath();

    /**
     * @return the file name of the upload as provided in the form submission.
     */
    String fileName();

    /**
     * @return the size of the upload, in bytes
     */
    long size();

    /**
     * @return the content type (MIME type) of the upload.
     */
    String contentType();

    /**
     * @return the charset of the upload.
     */
    String charSet();
}
