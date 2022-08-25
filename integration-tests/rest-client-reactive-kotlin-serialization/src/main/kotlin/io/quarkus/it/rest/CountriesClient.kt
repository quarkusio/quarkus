package io.quarkus.it.rest

import io.quarkus.rest.client.reactive.ClientExceptionMapper
import org.jboss.resteasy.reactive.ClientWebApplicationException
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

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

    @GET
    @Path("/notFoundCountries")
    @Produces(MediaType.APPLICATION_JSON)
    fun notFoundCountries(): List<Country>
    companion object {
        @JvmStatic
        @ClientExceptionMapper
        fun toException(response: Response): RuntimeException? {
            return when (response.status) {
                404 -> CountriesNotFoundException()
                else -> ClientWebApplicationException(response.status)
            }
        }
    }
}
