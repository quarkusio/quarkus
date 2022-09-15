package io.quarkus.it.shared;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/shared")
public class SharedResource {

    @GET
    public String shared() {
        return "Shared Resource";
    }

    //https://github.com/quarkusio/quarkus/issues/17175
    @GET
    @Path("/classloading")
    public String loadFromWrongClassLoader() throws Exception {
        //this is wrong, libraries should load from the Thread Context Class Loader
        //we test that even if libraries do the wrong thing our workaround still works
        //without the need to force flat Class-Path
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("wrong-classloading.txt")) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
