package io.quarkus.it.hibernate.validator.inheritance;

import javax.ws.rs.Path;

@Path(ScienceBookResource.PATH)
public interface ScienceBookResource extends BookResource {

    String PATH = BookResource.PATH + "/science";

}
