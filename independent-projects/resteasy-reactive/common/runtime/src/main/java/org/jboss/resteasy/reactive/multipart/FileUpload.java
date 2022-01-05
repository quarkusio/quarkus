package org.jboss.resteasy.reactive.multipart;

import java.nio.file.Path;

public interface FileUpload extends FilePart {

    default Path uploadedFile() {
        return filePath();
    }
}
