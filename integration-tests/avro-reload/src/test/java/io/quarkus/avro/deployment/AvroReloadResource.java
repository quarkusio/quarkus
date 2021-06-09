package io.quarkus.avro.deployment;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import test.Level;
import test.PrivacyImport;
import test.ProtocolPrivacy;

@Path("/")
public class AvroReloadResource {
    @GET
    public String getAvailablePrivacyImports() {
        return Arrays.stream(PrivacyImport.values()).map(String::valueOf).collect(joining(","));
    }

    @GET
    @Path("/protocol")
    public String getAvailableProtocolPrivacies() {
        return Arrays.stream(ProtocolPrivacy.values()).map(String::valueOf).collect(joining(","));
    }

    @GET
    @Path("/avdl")
    public String getAvailableLevel() {
        return Arrays.stream(Level.values()).map(String::valueOf).collect(joining(","));
    }

}
