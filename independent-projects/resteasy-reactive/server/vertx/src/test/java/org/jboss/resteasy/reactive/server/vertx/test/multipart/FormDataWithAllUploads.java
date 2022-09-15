package org.jboss.resteasy.reactive.server.vertx.test.multipart;

import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

public class FormDataWithAllUploads extends FormDataBase {

    @RestForm
    // don't set a part type, use the default
    private String name;

    @RestForm
    @PartType(MediaType.TEXT_PLAIN)
    private Status status;

    @RestForm
    private List<FileUpload> uploads;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<FileUpload> getUploads() {
        return uploads;
    }

    public void setUploads(List<FileUpload> uploads) {
        this.uploads = uploads;
    }
}
