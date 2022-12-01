package org.jboss.resteasy.reactive.multipart;

import java.nio.file.Path;

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
