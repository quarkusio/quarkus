package io.quarkus.it.smallrye.graphql;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;

import graphql.scalars.country.code.CountryCode;

@GraphQLApi
public class ExtendedScalarsResource {

    private final List<ExtendedScalars> scalars = new ArrayList<>();

    public ExtendedScalarsResource() throws MalformedURLException {
        ExtendedScalars sca = new ExtendedScalars();
        sca.setCurrency(Currency.getInstance("AUD"));
        sca.setId(UUID.randomUUID());
        sca.setCountryCode(CountryCode.AU);
        sca.setLocale(Locale.getDefault());
        sca.setUrl(new URL("Http://www.quarkus.io"));
        scalars.add(sca);
    }

    @Query
    public List<ExtendedScalars> scalars() {
        return scalars;
    }

    @Mutation
    public ExtendedScalars scalarsAdd(ExtendedScalars scalar) {
        this.scalars.add(scalar);
        return scalar;
    }

    @Mutation
    public ExtendedScalars scalarsAddId(UUID anId) {
        ExtendedScalars sca = new ExtendedScalars();
        sca.setCurrency(Currency.getInstance(Locale.getDefault()));
        sca.setId(anId);
        sca.setCountryCode(CountryCode.AU);
        sca.setLocale(Locale.getDefault());
        this.scalars.add(sca);
        return sca;
    }

    @Mutation
    public ExtendedScalars scalarsAddCurrency(Currency c) {
        ExtendedScalars sca = new ExtendedScalars();
        sca.setCurrency(c);
        sca.setId(UUID.randomUUID());
        sca.setCountryCode(CountryCode.AU);
        sca.setLocale(Locale.getDefault());
        this.scalars.add(sca);
        return sca;
    }

}
