package org.jboss.resteasy.reactive.multipart;

import java.nio.file.Path;

import jakarta.ws.rs.core.MultivaluedMap;

/**
 * Represent a file that has been uploaded.
 * <p>
 * This type is usually used on server, but it is also supported in the REST Client.
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

    /**
     * @return The headers that were present in the form submission.
     */
    MultivaluedMap<String, String> getHeaders();
}
