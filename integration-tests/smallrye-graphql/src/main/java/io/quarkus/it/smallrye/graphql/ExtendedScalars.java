package io.quarkus.it.smallrye.graphql;

import java.net.URL;
import java.util.Currency;
import java.util.Locale;
import java.util.UUID;

import graphql.scalars.country.code.CountryCode;

public class ExtendedScalars {

    private UUID id;
    private URL url;
    private Locale locale;
    private CountryCode countryCode;

    private Currency currency;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public CountryCode getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(CountryCode countryCode) {
        this.countryCode = countryCode;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    @Override
    public String toString() {
        return "ExtendedScalars{" +
                "id=" + id +
                ", url=" + url +
                ", locale=" + locale +
                ", countryCode=" + countryCode +
                ", currency=" + currency +
                '}';
    }
}
