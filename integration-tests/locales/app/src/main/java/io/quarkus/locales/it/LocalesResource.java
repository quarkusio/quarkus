package io.quarkus.locales.it;

import java.text.NumberFormat;
import java.text.ParseException;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TimeZone;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;

import io.smallrye.common.constraint.NotNull;

public class LocalesResource {

    private static final Logger LOG = Logger.getLogger(LocalesResource.class);

    @Path("/locale/{country}/{language}")
    @GET
    public Response inLocale(@PathParam("country") String country, @NotNull @PathParam("language") String language) {
        return Response.ok().entity(Locale.forLanguageTag(country).getDisplayCountry(new Locale(language))).build();
    }

    @Path("/default/{country}")
    @GET
    public Response inDefaultLocale(@PathParam("country") String country) {
        return Response.ok().entity(Locale.forLanguageTag(country).getDisplayCountry()).build();
    }

    @Path("/currency/{country}/{language}")
    @GET
    public Response currencyInLocale(@PathParam("country") String country, @NotNull @PathParam("language") String language) {
        return Response.ok().entity(Currency.getInstance(Locale.forLanguageTag(country)).getDisplayName(new Locale(language)))
                .build();
    }

    @Path("/timeZone")
    @GET
    public Response timeZoneInLocale(@NotNull @QueryParam("zone") String zone,
            @NotNull @QueryParam("language") String language) {
        return Response.ok().entity(TimeZone.getTimeZone(ZoneId.of(zone)).getDisplayName(new Locale(language))).build();
    }

    @Path("/numbers")
    @GET
    public Response decimalDotCommaLocale(@NotNull @QueryParam("locale") String locale,
            @NotNull @QueryParam("number") String number) throws ParseException {
        final Locale l = Locale.forLanguageTag(locale);
        LOG.infof("Locale: %s, Locale tag: %s, Number: %s", l, locale, number);
        return Response.ok().entity(String.valueOf(NumberFormat.getInstance(l).parse(number).doubleValue())).build();
    }

    @Path("/ranges")
    @GET
    public Response ranges(@NotNull @QueryParam("range") String range) {
        LOG.infof("Range: %s", range);
        return Response.ok().entity(Locale.LanguageRange.parse(range).toString()).build();
    }

    @Path("/message")
    @GET
    public Response message(@Context HttpHeaders headers) {
        final Locale locale = headers.getAcceptableLanguages().get(0);
        LOG.infof("Locale: %s, language: %s, country: %s", locale, locale.getLanguage(), locale.getCountry());
        return Response.ok().entity(ResourceBundle.getBundle("AppMessages", locale).getString("msg1")).build();
    }

}
