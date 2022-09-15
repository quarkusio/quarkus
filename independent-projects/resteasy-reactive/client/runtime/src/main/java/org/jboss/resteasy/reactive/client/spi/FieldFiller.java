package org.jboss.resteasy.reactive.client.spi;

import jakarta.ws.rs.core.GenericType;
import java.io.File;
import java.nio.file.Path;
import org.jboss.resteasy.reactive.multipart.FileDownload;

public abstract class FieldFiller {

    private final GenericType<?> fieldType;
    private final String partName;
    private final String mediaType;

    protected FieldFiller(GenericType<?> fieldType, String partName, String mediaType) {
        this.fieldType = fieldType;
        this.partName = partName;
        this.mediaType = mediaType;
    }

    public abstract void set(Object responseObject, Object fieldValue);

    public GenericType<?> getFieldType() {
        return fieldType;
    }

    public String getPartName() {
        return partName;
    }

    public String getMediaType() {
        return mediaType;
    }

    @SuppressWarnings("unused") // used in generated classes
    public static File fileDownloadToFile(FileDownload fileDownload) {
        return fileDownload.filePath().toFile();
    }

    @SuppressWarnings("unused") // used in generated classes
    public static Path fileDownloadToPath(FileDownload fileDownload) {
        return fileDownload.filePath();
    }
}
