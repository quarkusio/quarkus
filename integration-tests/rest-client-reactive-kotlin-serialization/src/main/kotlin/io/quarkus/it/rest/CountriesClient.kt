package io.quarkus.it.rest

import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("")
interface CountriesClient {
    @POST
    @Path("/country")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun country(country: Country): Country

    @GET
    @Path("/countries")
    @Produces(MediaType.APPLICATION_JSON)
    fun countries(): List<Country>
}