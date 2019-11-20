package io.quarkus.it.nativeannotations;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/class-init")
public class InitResource {

    @GET
    public String result() throws IOException {
        NativeFileClass.doClose();
        //this does not directly test anything, it just makes sure that everything has worked
        //as expected, and closes the file
        return "passed";
    }

}
