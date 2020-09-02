package io.quarkus.it.gcp.services;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/storage")
public class StorageResource {

    @Inject Storage storage;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String storage() {
        Bucket bucket = storage.get("quarkus-hello");
        Blob blob = bucket.get("hello.txt");
        return new String(blob.getContent());
    }

}