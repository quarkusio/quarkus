package io.quarkus.it.rest

import io.quarkus.rest.client.reactive.ClientExceptionMapper
import org.jboss.resteasy.reactive.ClientWebApplicationException
import javax.ws.rs.*
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
