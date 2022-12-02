package org.acme

import io.quarkus.qute.Template
import io.quarkus.qute.TemplateInstance
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Path("/some-page")
class SomePage(val page: Template) {

    @GET
    @Produces(MediaType.TEXT_HTML)
    operator fun get(@QueryParam("name") name: String?): TemplateInstance {
        return page.data("name", name)
    }
}