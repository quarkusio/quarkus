package io.quarkus.it.qute;

import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

@Path("json")
public class JsonResource {

    @Inject
    Template hello;

    @POST
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get(JsonObject request) {
        return hello.data("name", request.get("name"));
    }
}
