package org.jboss.resteasy.reactive.multipart;

import java.nio.file.Path;

/**
 * Represent a file that has been uploaded.
 * <p>
 * WARNING: This type is currently only supported on the server
 */
public interface FileUpload extends FilePart {

    /**
     * Use this constant as form parameter name in order to get all file uploads from a multipart form, regardless of their
     * names
     */
    public final static String ALL = "*";

    default Path uploadedFile() {
        return filePath();
    }
}
