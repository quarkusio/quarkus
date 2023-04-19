package org.acme

import io.quarkus.qute.Template

import groovy.transform.CompileStatic

import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@CompileStatic
@Path("/some-page")
class SomePage {

    @Inject
    Template page

    @GET
    @Produces(MediaType.TEXT_HTML)
    def get(@QueryParam("name") String name) {
        page.data("name", name)
    }

}
