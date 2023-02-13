package io.quarkus.it.hibernate.validator.inheritance;

import jakarta.ws.rs.Path;

@Path(ScienceBookResource.PATH)
public interface ScienceBookResource extends BookResource {

    String PATH = BookResource.PATH + "/science";

}
