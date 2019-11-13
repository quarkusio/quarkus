package io.quarkus.it.resteasy.jackson;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/article")
public class ArticleResource {

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Article create(Article input) {
        return input;
    }
}
