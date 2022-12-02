package io.quarkus.locales.it;

import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.TimeZone;

import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("")
public class LocalesResource {

    @Path("/locale/{country}/{language}")
    @GET
    public Response inLocale(@PathParam("country") String country, @NotNull @PathParam("language") String language) {
        return Response.ok().entity(
                Locale.forLanguageTag(country).getDisplayCountry(new Locale(language))).build();
    }

    @Path("/default/{country}")
    @GET
    public Response inDefaultLocale(@PathParam("country") String country) {
        return Response.ok().entity(
                Locale.forLanguageTag(country).getDisplayCountry()).build();
    }

    @Path("/currency/{country}/{language}")
    @GET
    public Response currencyInLocale(@PathParam("country") String country, @NotNull @PathParam("language") String language) {
        return Response.ok().entity(
                Currency.getInstance(Locale.forLanguageTag(country)).getDisplayName(new Locale(language))).build();
    }

    @Path("/timeZone")
    @GET
    public Response timeZoneInLocale(@NotNull @QueryParam("zone") String zone,
            @NotNull @QueryParam("language") String language) {
        return Response.ok().entity(
                TimeZone.getTimeZone(ZoneId.of(zone)).getDisplayName(new Locale(language))).build();
    }
}
