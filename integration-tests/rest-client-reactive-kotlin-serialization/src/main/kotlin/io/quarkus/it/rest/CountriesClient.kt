package io.quarkus.it.rest

import io.quarkus.rest.client.reactive.ClientExceptionMapper
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.jboss.resteasy.reactive.ClientWebApplicationException

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
