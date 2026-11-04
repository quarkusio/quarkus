package org.acme;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.runtime.configuration.ConfigUtils;

@Path("/profile")
public class ProfileResource {

    @GET
    public String profile() {
        return String.join(",", ConfigUtils.getProfiles());
    }
}
