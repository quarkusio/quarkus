package org.acme.qute

import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

@Path("/qute/hello")
class HelloResource {

    @Inject
    lateinit var hello: Template;

    @GET
    @Produces(MediaType.TEXT_HTML)
    fun get(@QueryParam("name") name: String?): TemplateInstance = hello.data("name", name)

}
